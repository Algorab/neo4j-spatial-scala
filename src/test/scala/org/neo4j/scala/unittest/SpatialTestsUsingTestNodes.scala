package org.neo4j.scala.unittest

import org.neo4j.gis.spatial.NullListener
import collection.mutable.Buffer
import com.vividsolutions.jts.geom.Envelope
import org.neo4j.gis.spatial.query.SearchWithin
import org.specs2.mutable.SpecificationWithJUnit
import org.neo4j.scala.{SimpleSpatialDatabaseServiceProvider, SpatialDatabaseServiceProvider, EmbeddedGraphDatabaseServiceProvider, Neo4jSpatialWrapper}

class SpatialTestsUsingTestNodes extends SpecificationWithJUnit with Neo4jSpatialWrapper with EmbeddedGraphDatabaseServiceProvider with SimpleSpatialDatabaseServiceProvider {

  def neo4jStoreDir = "/tmp/temp-neo-spatial-test2"

  "NeoSpatialWrapper" should {

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run() {
        ds.gds.shutdown
      }
    })

    "allow usage of common pattern like those in TestNodes.scala" in {

      withSpatialTx {
        implicit db =>

        // remove existing layer
          try {
            deleteLayer("test", new NullListener)
          }
          catch {
            case _ =>
          }

        val cities = createNode
        val federalStates = createNode

        withLayer(getOrCreateEditableLayer("test")) {
          implicit layer =>

          /**
           * create Munich and "attach" it to the cities node
           */
          val munich = NewSpatialNode[City]((15.3, 56.2))
          munich.name = "Munich"
          cities --> "isCity" --> munich

          /**
           * create a polygon called Bayern, "attach" it to the federal state node and
           * "attach" the capital city Munich
           */
          val bayernBuffer = Buffer[(Double, Double)]((15, 56), (16, 56), (15, 57), (16, 57), (15, 56))
          val bayern = NewSpatialNode[FedaralState](bayernBuffer)
          bayern.name = "Bayern"
          federalStates --> "isFederalState" --> bayern
          munich --> "CapitalCityOf" --> bayern

          /**
           * search all geometries inside an Envelope
           */
          var result = for (r <- search[SearchWithin](toGeometry(new Envelope(15.0, 16.0, 56.0, 57.0)))) yield r
          result.size must_== 2

          /**
           * retrieve the capital city of Bayern
           */
          bayern.getCapitalCity.name must beEqualTo(munich.name)
        }
      }
    }
  }
}
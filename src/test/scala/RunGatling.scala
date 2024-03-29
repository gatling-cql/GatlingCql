/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 GatlingCql developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import java.io.File
import io.gatling.app.Gatling
import io.gatling.core.config.GatlingPropertiesBuilder

/**
 * Helper class to run Gatling simulation class (not suitable for unit tests).
 */
object RunGatling extends App
{
  val projectRootDir = new File(".").toPath

  val mavenResourcesDirectory = projectRootDir.resolve("src").resolve( "test").resolve( "resources")
  val mavenTargetDirectory = projectRootDir.resolve("target")
  val mavenBinariesDirectory = mavenTargetDirectory.resolve("test-classes")
  val resultsDirectory = mavenTargetDirectory.resolve("results")

  val props = new GatlingPropertiesBuilder

  props.resultsDirectory(resultsDirectory.toString)
  props.binariesDirectory(mavenBinariesDirectory.toString)

  props.simulationClass("io.github.gatling.cql.CheckCompileTest")

  Gatling.fromMap(props.build)
}

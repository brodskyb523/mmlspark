// Copyright (C) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See LICENSE in project root for information.

package com.microsoft.ml.spark.cntk

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.{ByteArrayOutputStream, File}

import javax.imageio.ImageIO
import com.microsoft.ml.spark.IO.image.ImageReader
import org.apache.spark.sql.functions.{col, udf}
import org.apache.spark.sql.types.StringType
import org.opencv.core.Core

import scala.util.Random

class SuperpixelSuite extends CNTKTestUtils {

  lazy val sp1 = new Superpixel(img, 16, 130)
  lazy val sp2 = new Superpixel(img2, 100, 130)
  lazy val width = 300
  lazy val height = 300
  lazy val rgbArray = new Array[Int](width * height)
  lazy val img: BufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
  lazy val img2: BufferedImage = ImageIO.read(
    new File("/home/bebr/lib/datasets/Images/Grocery/testImages/WIN_20160803_12_37_07_Pro.jpg"))

  // Adds colors to the img
  for (y <- 0 until height) {
    val red = (y * 255) / (height - 1)
    for (x <- 0 until width) {
      val green = (x * 255) / (width - 1)
      val blue = 128
      rgbArray(x + y * height) = (red << 16) | (green << 8) | blue
    }
  }
  img.setRGB(0, 0, width, height, rgbArray, 0, width)

  lazy val allClusters: Array[Cluster] = sp1.clusters
  lazy val allClusters2: Array[Cluster] = sp2.clusters
  lazy val states: Array[Boolean] = Array.fill(allClusters.length) {
    Random.nextDouble() > 0.5
  }
  lazy val states2: Array[Boolean] = Array.fill(allClusters2.length) {
    Random.nextDouble() > 0.5
  }

  val superpixels: SuperpixelData = SuperpixelData.fromSuperpixel(sp1)
  val superpixels2: SuperpixelData = SuperpixelData.fromSuperpixel(sp2)

  ImageReader.OpenCVLoader
  lazy val censoredImg: BufferedImage = Superpixel.censorImage(
    ImageReader.decode(img).get, superpixels, states)
  lazy val censoredImg2: BufferedImage = Superpixel.censorImage(
    ImageReader.decode(img2).get, superpixels2, states2)

  test("ToList should work on an iterator") {
    val sampler = Superpixel.clusterStateSampler(0.3, 1000)
    val samples: List[Array[Boolean]] = sampler.take(10).toList
    assert(samples.size === 10)
  }

  test("GetClusteredImage should show the image with its clusters outlined, not censored") {
    Superpixel.displayImage(sp1.getClusteredImage)
    Superpixel.displayImage(censoredImg)
    Superpixel.displayImage(sp2.getClusteredImage)
    Superpixel.displayImage(censoredImg2)
    Thread.sleep(100000)
  }

//  // TODO - Change file path
//  test("Superpixeling should work properly on grocery img") {
//    val groceryImg: BufferedImage = ImageIO.read(
//      new File("/home/bebr/lib/datasets/Images/Grocery/testImages/WIN_20160803_12_37_07_Pro.jpg"))
//    val spGrocery = new Superpixel(groceryImg, 100, 130)
//    Superpixel.displayImage(spGrocery.getClusteredImage())
//    Thread.sleep(180000)
//  }

  test("Censored clusters' pixels should be black in the censored image") {
    for (i <- states.indices if !states(i)) {
      allClusters(i).pixels.foreach { case (x: Int, y: Int) =>
        val color = new Color(censoredImg.getRGB(x, y))
        assert(color.getRed === 0 && color.getGreen === 0 && color.getBlue === 0)
      }
    }
  }

  test("image censoring udf"){
    import session.implicits._
    val df = List.fill(100)((2, "foo")).toDF("foo","bar")

    def simpleFunc(x: Int, y:String): String = y*x

    val df2 = df.withColumn("baz", udf(simpleFunc _, StringType)(col("foo"), col("bar")))
    df2.show()

  }

  test("superpixel transformer works"){

  }

//  test("The correct censored image gets created from clusters and their states") {
//    val outputImg = randomClusters._1
//    val imageFromStates = Superpixel.createImage(img, randomClusters._2, randomClusters._3)
//
//    assert(outputImg.getWidth === imageFromStates.getWidth &&
//      outputImg.getHeight === imageFromStates.getHeight)
//
//    for (x <- 0 until outputImg.getWidth) {
//      for (y <- 0 until outputImg.getHeight) {
//        assert(outputImg.getRGB(x, y) === imageFromStates.getRGB(x, y))
//      }
//    }
//  }
}
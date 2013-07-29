/*
*
* This file is shamelessly copied from 
* https://github.com/thesamet/kdtree-scala
*/

package smptk.mesh.kdtree

import scala.math.Numeric.Implicits._
import smptk.geometry.{Point, ThreeD}

/** Metric is a trait whose instances each represent a way to measure distances between
  * instances of a type.
  *
  * `A` represents the type of the points and `R` represents the metric value.
  */
trait Metric[A, R] {
  /** Returns the distance between two points. */
  def distance(x: A, y: A): R

  /** Returns the distance between x and a hyperplane that passes through y and perpendicular to
    * that dimension.
    */
  def planarDistance(dimension: Int)(x: A, y: A): R
}

object Metric {
  implicit def metricFromTuple2[A](implicit n: Numeric[A]) = new Metric[(A, A), A] {
    def distance(x: (A, A), y: (A, A)): A = {
      val dx = (x._1 - y._1)
      val dy = (x._2 - y._2)
      dx * dx + dy * dy
    }

    def planarDistance(d: Int)(x: (A, A), y: (A, A)): A = {
      val dd = x.productElement(d).asInstanceOf[A] - y.productElement(d).asInstanceOf[A]
      dd * dd
    }
  }


  implicit def metricFromTuple3[A](implicit n: Numeric[A]) = new Metric[(A, A, A), A] {
    def distance(x: (A, A, A), y: (A, A, A)): A = {
      val dx = (x._1 - y._1)
      val dy = (x._2 - y._2)
      val dz = (x._3 - y._3)
      dx * dx + dy * dy + dz * dz
    }

    def planarDistance(d: Int)(x: (A, A, A), y: (A, A, A)): A = {
      val dd = x.productElement(d).asInstanceOf[A] - y.productElement(d).asInstanceOf[A]
      dd * dd
    }
  }


  implicit def metricFromCoordVector3D(implicit n: Numeric[Double]) = new Metric[Point[ThreeD], Double] {
    def distance(x: Point[ThreeD], y: Point[ThreeD]): Double = {
      val dx = (x(0) - y(0))
      val dy = (x(1) - y(1))
      val dz = (x(2) - y(2))
      dx * dx + dy * dy + dz * dz
    }

    def planarDistance(d: Int)(x : Point[ThreeD], y : Point[ThreeD]): Double = {

      val dd = x(d) - y(d)
      dd * dd
    }
  }


}
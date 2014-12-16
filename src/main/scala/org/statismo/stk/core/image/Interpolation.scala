package org.statismo.stk.core.image

import org.statismo.stk.core.geometry._

import scala.language.implicitConversions
import breeze.linalg.{DenseVector}
import scala.reflect.runtime.universe.{ TypeTag }
import org.statismo.stk.core.common.ScalarValue



object Interpolation {

  private val twoByThree = 2.0 / 3.0; // a constant used later on
    
  
  /** 
   * performs a b-spline interpolation of given degree of a 1D image 
   * */ 
  def interpolate[@specialized(Short, Int, Float, Double) Scalar : ScalarValue](image: DiscreteScalarImage1D[Scalar], degree: Int): ContinuousScalarImage1D = {

    val ck = determineCoefficients(degree, image)

    /*
     * Computes values at given point with corresponding coefficients and spline basis
     * */
    def iterateOnPoints(x: Point[_1D], splineBasis: ((Double) => Double)): Double = {
      val xUnit = (x(0) - image.domain.origin(0)) / image.domain.spacing(0)

      val k1 = scala.math.ceil(xUnit - 0.5f * (degree + 1)).toInt
      val K =degree + 1

      var result = 0.0
      var k = k1
      while (k <=  k1 + K - 1) {
    	val kBC = applyMirrorBoundaryCondition(k, image.domain.size(0))
        result = result + splineBasis(xUnit.toDouble - k) * ck(kBC)
        k = k + 1
      }
      result
    }

    // the continuous interpolation function
    def f(x: Point[_1D]) = {
        val splineBasis: (Double => Double) = bSpline(degree) 
        iterateOnPoints(x, splineBasis).toFloat
      }
    // the derivative
    def df(x: Point[_1D]) = { //derivative
        val splineBasisD1: (Double => Double) = { x => (bSpline(degree - 1)(x + 0.5f) - bSpline(degree - 1)(x - 0.5f)) * (1/image.domain.spacing(0)) }
        Vector(iterateOnPoints(x, splineBasisD1).toFloat)
      }    
    new ContinuousScalarImage1D(image.domain,   f, Some(df))
  }


   /** 
   * performs a b-spline interpolation of given degree of a 2D image 
   * */ 
  def interpolate[@specialized(Short, Int, Float, Double) Scalar : ScalarValue](image: DiscreteScalarImage2D[Scalar], degree: Int): ContinuousScalarImage2D = {
    val ck = determineCoefficients(degree, image)

    def iterateOnPoints( x: Point[_2D], splineBasis: ((Double, Double) => Double)): Double = {
      val xUnit = ((x(0) - image.domain.origin(0)) / image.domain.spacing(0))
      val yUnit = ((x(1) - image.domain.origin(1)) / image.domain.spacing(1))

      val k1 = scala.math.ceil(xUnit - 0.5f * (degree + 1)).toInt
      val l1 = scala.math.ceil(yUnit - 0.5f * (degree + 1)).toInt

      val K = degree + 1

      var result = 0.0
      var l = l1
      while (l <= l1 + K - 1) {
        val lBC = applyMirrorBoundaryCondition(l, image.domain.size(1))        		
        var k = k1
        while (k <= k1 + K - 1) { 
        	val kBC = applyMirrorBoundaryCondition(k, image.domain.size(0))
          val idx = image.domain.indexToLinearIndex(Index(kBC, lBC))
          result = result + ck(idx) * splineBasis(xUnit - k, yUnit - l)
          k = k + 1
        }
        l = l + 1
      }
      result
    }

    val bSplineNthOrder = bSpline(degree)_
    val bSplineNmin1thOrder = bSpline(degree - 1)_

    def f(x: Point[_2D]) = {
        val splineBasis = (x: Double, y: Double) => bSplineNthOrder(x) * bSplineNthOrder(y) // apply function
        iterateOnPoints( x,  splineBasis).toFloat      
    }
    def df(x: Point[_2D]) = { //derivative
        val splineBasisD1 = (x: Double, y: Double) => (bSplineNmin1thOrder(x + 0.5f) - bSplineNmin1thOrder(x - 0.5f)) * bSplineNthOrder(y)
        val splineBasisD2 = (x: Double, y: Double) => bSplineNthOrder(x) * (bSplineNmin1thOrder(y + 0.5f) - bSplineNmin1thOrder(y - 0.5f))
        val dfx = (iterateOnPoints( x,  splineBasisD1)* (1/image.domain.spacing(0))).toFloat
        val dfy = (iterateOnPoints(x,splineBasisD2)* (1/image.domain.spacing(1))).toFloat
        Vector(dfx, dfy)
      }

    new ContinuousScalarImage2D(image.domain,f, Some(df))
  }

  
   /** 
   * performs a b-spline interpolation of given degree of a 3D image 
   * */   
  def interpolate[@specialized(Short, Int, Float, Double) Scalar : ScalarValue](image: DiscreteScalarImage3D[Scalar], degree: Int): ContinuousScalarImage3D = {
    val ck = determineCoefficients(degree, image)

    def iterateOnPoints(x: Point[_3D], splineBasis: ((Double, Double, Double) => Double)): Double = {
      val xUnit = ((x(0) - image.domain.origin(0)) / image.domain.spacing(0))
      val yUnit = ((x(1) - image.domain.origin(1)) / image.domain.spacing(1))
      val zUnit = ((x(2) - image.domain.origin(2)) / image.domain.spacing(2))

      val k1 = scala.math.ceil(xUnit - 0.5f * (degree + 1)).toInt
      val l1 = scala.math.ceil(yUnit - 0.5f * (degree + 1)).toInt
      val m1 = scala.math.ceil(zUnit - 0.5f * (degree + 1)).toInt

      val K = degree + 1

      var result = 0.0
      var k = k1
      var l = l1
      var m = m1
              
      while (m <= m1 + K - 1) {
        val mBC = applyMirrorBoundaryCondition(m, image.domain.size(2))
        l = l1
        while (l <= l1 + K - 1) {
          val lBC = applyMirrorBoundaryCondition(l, image.domain.size(1))        	
          k = k1
          while (k <=  k1 + K - 1) {
            val kBC = applyMirrorBoundaryCondition(k, image.domain.size(0))
            val idx = image.domain.indexToLinearIndex(Index(kBC, lBC, mBC))
            result = result + ck(idx) * splineBasis(xUnit - k, yUnit - l, zUnit - m)
            k = k + 1
          }
          l = l + 1
        }
        m = m + 1
      }
      result
    }

    val bSplineNthOrder = bSpline(degree)_
    val bSplineNmin1thOrder = bSpline(degree - 1)_

    def f(x: Point[_3D]) = {
        val splineBasis = (x: Double, y: Double, z: Double) => bSplineNthOrder(x) * bSplineNthOrder(y) * bSplineNthOrder(z)
        iterateOnPoints(x, splineBasis).toFloat
    }
    def df(x: Point[_3D]) = {
        val splineBasisD1 = (x: Double, y: Double, z: Double) => (bSplineNmin1thOrder(x + 0.5f) - bSplineNmin1thOrder(x - 0.5f)) * bSplineNthOrder(y) * bSplineNthOrder(z)
        val splineBasisD2 = (x: Double, y: Double, z: Double) => bSplineNthOrder(x) * (bSplineNmin1thOrder(y + 0.5f) - bSplineNmin1thOrder(y - 0.5f)) * bSplineNthOrder(z)
        val splineBasisD3 = (x: Double, y: Double, z: Double) => bSplineNthOrder(x) * bSplineNthOrder(y) * (bSplineNmin1thOrder(z + 0.5f) - bSplineNmin1thOrder(z - 0.5f))
        val dfx = (iterateOnPoints(x, splineBasisD1)* (1/image.domain.spacing(0))).toFloat
        val dfy = (iterateOnPoints(x, splineBasisD2)* (1/image.domain.spacing(1))).toFloat
        val dfz = (iterateOnPoints(x, splineBasisD3)* (1/image.domain.spacing(2))).toFloat
        Vector(dfx, dfy, dfz)
    }
    new ContinuousScalarImage3D(image.domain, f, Some(df))
  }

  
  /* determine the b-spline coefficients for a 1D image */
  def determineCoefficients[@specialized(Short, Int, Float, Double) Pixel : ScalarValue](degree: Int, img: DiscreteScalarImage1D[Pixel]): Array[Float] = {
    val ScalarValue = implicitly[ScalarValue[Pixel]]

    // the c is an input-output argument here
    val c = img.values.map(ScalarValue.toFloat)
    BSplineCoefficients.getSplineInterpolationCoefficients(degree, c)
    c
  }


  /* determine the b-spline coefficients for a 2D image. The coefficients are retunred
   * as a DenseVector, i.e. the rows are written one after another */
  def determineCoefficients[@specialized(Short, Int, Float, Double) Pixel : ScalarValue](degree: Int, img: DiscreteScalarImage2D[Pixel]): Array[Float] = {
	val ScalarValue = implicitly[ScalarValue[Pixel]]
    val coeffs = DenseVector.zeros[Float](img.values.size)
    var y = 0
    while (y < img.domain.size(1)) {
      val rowValues = (0 until img.domain.size(0)).map(x => img.values(img.domain.indexToLinearIndex(Index(x, y))))

      // the c is an input-output argument here
      val c = rowValues.map(ScalarValue.toFloat).toArray
      BSplineCoefficients.getSplineInterpolationCoefficients(degree, c)

      val idxInCoeffs = img.domain.indexToLinearIndex(Index(0, y))
      coeffs(idxInCoeffs until idxInCoeffs + img.domain.size(0)) := DenseVector(c)
      y=y+1
    }
    coeffs.data
  }

  /* determine the b-spline coefficients for a 3D image. The coefficients are returned
   * as a DenseVector, i.e. the slices and rows are written one after another */

  def determineCoefficients[@specialized(Short, Int, Float, Double) Pixel : ScalarValue](degree: Int, img: DiscreteScalarImage3D[Pixel]): Array[Float] = {
    val ScalarValue = implicitly[ScalarValue[Pixel]]
    val coeffs = DenseVector.zeros[Float](img.values.size)
    var z = 0
    var y = 0
    while (z < img.domain.size(2)) {   
      y = 0
      while (y < img.domain.size(1)) {
        val rowValues = (0 until img.domain.size(0)).map(x => img.values(img.domain.indexToLinearIndex(Index(x, y, z))))

        // the c is an input-output argument here
        val c = rowValues.map(ScalarValue.toFloat).toArray
        BSplineCoefficients.getSplineInterpolationCoefficients(degree, c)
        val idxInCoeffs = img.domain.indexToLinearIndex(Index(0, y, z))
        coeffs(idxInCoeffs until idxInCoeffs + img.domain.size(0)) := DenseVector(c)
        y=y+1
      }
      z=z+1
    }
    coeffs.data
  }  
  
  
  /** 
   * The bspline basis function for degrees 0 to 3 
   **/
  def bSpline(n: Int)(x: Double): Double = {
    val absX : Double = scala.math.abs(x)
    val absXSquared : Double = absX * absX
    val absXCube : Double = absXSquared * absX
    val twoMinAbsX : Double = 2.0 - absX
    
    n match {
      case 0 => {
        if (-0.5 < x && x < 0.5) 1.0
        else if (absX == 0.5) 0.5
        else 0

      }
      case 1 => {
        if (-1 <= x && x <= 0) 1.0 + x
        else if (0 < x && x <= 1) 1.0 - x
        else 0
      }
      case 2 => {
        if (-1.5 <= x && x < -0.5) 0.5 * (x + 1.5)*(x + 1.5)
        else if  (-0.5 <= x && x < 0.5) -(x + 0.5)*(x + 0.5) + (x - 0.5) + 1.5
        else if (x >= 0.5 && x < 1.5) 0.5 * (1 - (x - 0.5))*(1 - (x - 0.5))
        else 0

      }
      case 3 => {

        if (absX >= 0 && absX < 1)
          twoByThree - absXSquared + 0.5 * absXCube
        else if (absX >= 1 && absX < 2)
        	twoMinAbsX * twoMinAbsX *  twoMinAbsX / 6.0
        else 0
      }
      case _ => throw new NotImplementedError("Bspline of order " + n + " is not implemented yet")
    }
  }

  
  
  
  /**
   *  computes the right index for the coefficient, 
   * taking the boundary conditions into account (it mirrors at the border)
   **/
  private def applyMirrorBoundaryCondition(k : Int, numCoefficients : Int) = {    
	  if (k < 0) -k
      else if (k >= numCoefficients) numCoefficients -(k - numCoefficients) - 2
      else k        
  }
    


}
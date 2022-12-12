package com.example.myapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener{
    private var mLayout: LinearLayout? = null
    private var imageLayout: RelativeLayout? = null
    private var img: String? = null
    private var img1: ImageView? = null
    private var img2: ImageView? = null
    private var mtx: Mat? = null
    private var dstCoeff: Mat? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OpenCVLoader.initDebug()
        setContentView(R.layout.activity_main)

        // get UI elements
        mLayout = findViewById<LinearLayout>(R.id.layout)
        imageLayout = findViewById<RelativeLayout>(R.id.images)
        img1 = findViewById<ImageView>(R.id.first_image)
        img2 = findViewById<ImageView>(R.id.second_image)
        val filterButton = findViewById<Button>(R.id.select_button)
        val spinner = findViewById<Spinner>(R.id.spinner)
        spinner.onItemSelectedListener = this

        // set image options in dropdown menu
        val options: MutableList<String> = ArrayList()
        options.add("image001")
        options.add("image002")
        options.add("image003")
        options.add("image004")
        options.add("image005")
        options.add("image006")
        options.add("image007")
        options.add("image008")
        val dataAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = dataAdapter

        // calibrate camera at the beginning
        calibrateImage()

        // process current image by clicking on the 'filter image' button
        filterButton.setOnClickListener {
            runImagePipeline()
        }

    }

    /**
     * function that is executed when selecting an item in the dropdown list
     * -> Auto-generated method
     */
    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        // Selecting a new image in the dropdown menu
        val item = parent.getItemAtPosition(position).toString()
        img = item
        setImage(img!!)
    }

    /**
     * function that is executed when nothing is selected in the dropdown list
     * -> Auto-generated method
     */
    override fun onNothingSelected(arg0: AdapterView<*>?) {
        // TODO Auto-generated method stub
    }

    /**
     * function that displays the selected image on the screen and removes the old processed image
     */
    private fun setImage(pathName: String) {
        // get image based on image name
        val res = resources.getIdentifier("$img", "drawable", this.packageName)
        val bitmap = BitmapFactory.decodeResource(resources, res)

        // display selected image on the screen
        img1!!.setImageBitmap(bitmap)

        // remove the olf processed image
        img2?.setImageBitmap(null)
    }

    /**
     * function that is executed when the user clicks on the 'filter image' button
     * the function processes the selected image and adds a new image with the marked lanes to the screen
     */
    private fun runImagePipeline() {
        // get image based on the selected image name
        val res = resources.getIdentifier("$img", "drawable", this.packageName)
        val originalBitmap = BitmapFactory.decodeResource(resources, res)

        // store original image as mat object
        val originalMat = Mat()
        Utils.bitmapToMat(originalBitmap, originalMat)

        // undistort image using predefined Camera Matrix
        val calib = Mat()
        Calib3d.undistort(originalMat, calib, mtx, dstCoeff)

        // Define perspective transformation corners
        val src = MatOfPoint2f(
            Point(560.0*2.75, 450.0*2.75),
            Point(720.0*2.75, 450.0*2.75),
            Point(200.0*2.75, 700.0*2.75),
            Point(1200.0*2.75, 700.0*2.75)
        )
        val dst = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(originalMat.width().toDouble(), 0.0),
            Point(0.0, originalMat.height().toDouble()),
            Point(originalMat.width().toDouble(), originalMat.height().toDouble())
        )

        // Apply perspective transformation
        val m = Imgproc.getPerspectiveTransform(src, dst)
        val warpedImage = Mat()
        val size = Size(originalBitmap.width.toDouble(), originalBitmap.height.toDouble())
        Imgproc.warpPerspective(calib, warpedImage, m, size)

        // Apply filters to find lines
        var filteredImage = filterImage(warpedImage)

        // Split image and filter for single points
        val pair = splitImageInLeftAndRightLine(filteredImage)

        // get representative polynomial function for each half
        val leftLines = getCorrectedLines(pair.first!!)
        val rightLines = getCorrectedLines(pair.second!!)

        // draw the lines and the space between the lines into the calibrated image
        val originalOverlayed = addLinesToImage(calib, leftLines, rightLines)

        // add processed image to the screen
        val processedBitmap = Bitmap.createBitmap(originalOverlayed!!.cols(), originalOverlayed!!.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(originalOverlayed, processedBitmap)
        img2!!.setImageBitmap(processedBitmap)
    }

    /**
     * function that calibrates the camera based on calibration files
     */
    private fun calibrateImage() {
        // get calibration images
        val images = ArrayList<Bitmap>()
        for (i in 1..20) {
            val res = resources.getIdentifier("calibration${i.toString()}", "drawable", this.packageName)
            val image = BitmapFactory.decodeResource(resources, res)
            images.add(image)
        }
        // termination criteria
        val criteria = TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 30, 0.001)

        val cols = 9
        val rows = 6
        val objp = Mat(1, (cols * rows), CvType.CV_32FC3)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                objp.put(0, (i * cols + j), j.toDouble(), i.toDouble(), 0.0)
            }
        }

        // arrays to store object points and image points
        val objPoints = ArrayList<Mat>()
        val imgPoints = ArrayList<Mat>()
        val imageSize = Size()

        images.forEach { image ->
            val mat = Mat()
            Utils.bitmapToMat(image, mat)

            // convert to gray image
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)

            // find the chess boars corners
            val corners = MatOfPoint2f()
            val found = Calib3d.findChessboardCorners(gray, Size(cols.toDouble(), rows.toDouble()), corners)

            // if found, add object points, image points (after refining them)
            if (found) {
                Imgproc.cornerSubPix(gray, corners, Size(11.0, 11.0), Size(-1.0, -1.0), criteria)
                objPoints.add(objp)
                imgPoints.add(corners)
                imageSize.width = gray.width().toDouble()
                imageSize.height = gray.height().toDouble()
            }
        }


        // set camera matrix and distribution used for calibrating and undistorting camera images
        val cameraMatrix = Mat(3, 3, CvType.CV_64FC1)
        val distCoeffs = Mat(5, 1, CvType.CV_64FC1)
        val rvecs = ArrayList<Mat>()
        val tvecs = ArrayList<Mat>()
        Calib3d.calibrateCamera(objPoints, imgPoints, imageSize, cameraMatrix, distCoeffs, rvecs, tvecs)
        mtx = cameraMatrix
        dstCoeff = distCoeffs

        Toast.makeText(this@MainActivity, "Camera Calibrated", Toast.LENGTH_SHORT).show()
    }

    /**
     * function that draws the left and right lane on a given image and fills the space between the two lanes
     *
     * @param image original image
     * @param leftLines arraylist with points representing the left lane
     * @param right_lines arraylist with points representing the right lane
     *
     * @return mat object that contains the original image with the marked lanes and the filled space between the lanes
     */
    private fun addLinesToImage(image: Mat, leftLines: ArrayList<Point>, rightLines: ArrayList<Point>): Mat {
        // Shift right curve to the right to display it correctly
        rightLines.forEach {
            it.x = it.x + image.width()/2
        }

        // reverse right line to display the polygon correctly
        rightLines.reverse()

        // create array list containing left and right line
        val combined = (leftLines + rightLines) as ArrayList<Point>

        // init black image
        val overlay = Mat(image.height(), image.width(), CvType.CV_8UC4, Scalar(0.0, 0.0, 0.0))

        // fill space between lines with (green) polygon
        val pts = MatOfPoint()
        pts.fromList(combined)
        val contours = ArrayList<MatOfPoint>()
        contours.add(pts)
        Imgproc.fillPoly(overlay, contours, Scalar(0.0, 255.0, 0.0))

        // Draw both lines in image
        combined.forEach {
            Imgproc.circle(overlay, it, 2, Scalar(0.0, 0.0, 255.0), 180)
        }

        // transform polygon back to original proportions
        val src = MatOfPoint2f(
            Point(560.0*2.75, 450.0*2.75),
            Point(720.0*2.75, 450.0*2.75),
            Point(200.0*2.75, 700.0*2.75),
            Point(1200.0*2.75, 700.0*2.75)
        )
        val dst = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(image.width().toDouble(), 0.0),
            Point(0.0, image.height().toDouble()),
            Point(image.width().toDouble(), image.height().toDouble())
        )
        val m = Imgproc.getPerspectiveTransform(dst, src)
        val transformedBack = Mat()
        val size = Size(image.width().toDouble(), image.height().toDouble())
        Imgproc.warpPerspective(overlay, transformedBack, m, size)

        // overlay polygon with limited opacity onto road and store it as mat object
        val originalOverlayed = Mat()
        Core.addWeighted( image, 1.0, transformedBack, 0.5, 0.0, originalOverlayed);

        return originalOverlayed
    }

    /**
     * function that filters the yellow and white lines of an image in HLS color space
     *
     * @param mat image as mat object
     *
     * @return mat object with the filtered white and yellow lines
     */
    private fun filterImage(mat: Mat): Mat? {
        // convert to hls color space
        val hls = Mat()
        Imgproc.cvtColor(mat, hls, Imgproc.COLOR_RGB2HLS)

        // filter white lines with lightness value
        val lowerWhite = Scalar(0.0, 210.0, 0.0)
        val upperWhite = Scalar(255.0, 255.0, 255.0)
        val whiteMask = Mat()
        Core.inRange(hls, lowerWhite, upperWhite, whiteMask)

        // filter yellow lines with saturation and hue value
        val lowerYellow = Scalar(17.0, 0.0, 89.0)
        val upperYellow = Scalar(46.0, 255.0, 255.0)
        val yellowMask = Mat()
        Core.inRange(hls, lowerYellow, upperYellow, yellowMask)

        // combine yellow and white filter
        val combined = Mat()
        Core.bitwise_or(whiteMask, yellowMask, combined)

        return combined
    }

    /**
     * function that splits an image into the left and the right half to separate the two lanes
     * and filters for single points
     *
     * @param mat image as mat object
     *
     * @return pair of mat objects representing left and right half of image
     */
    private fun splitImageInLeftAndRightLine(mat: Mat?): Pair<Mat?, Mat?> {
        // split image in left and right half
        var leftHalf = mat!!.colRange(0, mat!!.width()/2)
        var rightHalf = mat!!.colRange(mat!!.width()/2, mat!!.width())

        // define left kernel
        val leftKernel = Mat(3, 3, CvType.CV_32F)
        leftKernel.put(0, 0, 1.0)
        leftKernel.put(0, 1, 0.0)
        leftKernel.put(0, 2, -1.0)
        leftKernel.put(1, 0, 1.0)
        leftKernel.put(1, 1, 0.0)
        leftKernel.put(1, 2, -1.0)
        leftKernel.put(2, 0, 1.0)
        leftKernel.put(2, 1, 0.0)
        leftKernel.put(2, 2, -1.0)

        // filter left half for single points
        val left = Mat()
        Imgproc.filter2D(leftHalf, left, -1, leftKernel)

        // define right kernel
        val rightKernel = Mat(3, 3, CvType.CV_32F)
        rightKernel.put(0, 0, -1.0)
        rightKernel.put(0, 1, 0.0)
        rightKernel.put(0, 2, 1.0)
        rightKernel.put(1, 0, -1.0)
        rightKernel.put(1, 1, 0.0)
        rightKernel.put(1, 2, 1.0)
        rightKernel.put(2, 0, -1.0)
        rightKernel.put(2, 1, 0.0)
        rightKernel.put(2, 2, 1.0)

        // filter right half for single points
        val right = Mat()
        Imgproc.filter2D(rightHalf, right, -1, rightKernel)

        return Pair(left, right)
    }

    /**
     * function that returns an array with the coordinates of all white pixels of an image
     *
     * @param mat image as mat object
     *
     * @return array with coordinates of white pixel
     */
    private fun getArrayList(mat: Mat): Array<Point> {
        // filter points that are not zero
        val whiteMat = Mat()
        Core.findNonZero(mat, whiteMat)
        // return array with coordinates of white points
        return Array(whiteMat.rows()) {
            Point(whiteMat.get(it, 0)[0], whiteMat.get(it, 0)[1])
        }
    }

    /***
     * function that calculates the correct lane of an image with polynomial regression
     *
     * @param mat image as mat object
     *
     * @return arraylist with correct lane coordinates
     */
    private fun getCorrectedLines(mat: Mat): ArrayList<Point> {
        // Determine coefficients of the polynomial Function representing a line
        val pointList = getArrayList(mat)
        val coefficients = polynomRegression(pointList)

        val points = ArrayList<Point>()
        val c = coefficients[0]
        val b = coefficients[1]
        val a = coefficients[2]

        // Get representative Polynomial function
        val yValues = Array(mat.height()) { i -> i + 1 }
        yValues.forEach {
            val y1 = a * it * it + b * it + c
            points.add(Point(y1, it.toDouble()))
        }
        return points
    }

    /**
     * function that determines the second degree polynomial function of coordinates
     * source: https://rosettacode.org/wiki/Polynomial_regression#Kotlin
     *
     * @param pointList array with the coordinates
     *
     * @return double-array with the coefficients of the polynomial function
     */
    private fun polynomRegression(pointList: Array<Point>): DoubleArray {
        val y = DoubleArray(0).toMutableList()
        val x = DoubleArray(0).toMutableList()

        // swap x and y values to get correct polynomial function
        pointList.forEach {
            x.add(it.y)
            y.add(it.x)
        }

        val xm = x.average()
        val ym = y.average()
        val x2m = x.map { it * it }.average()
        val x3m = x.map { it * it * it }.average()
        val x4m = x.map { it * it * it * it }.average()
        val xym = x.zip(y).map { it.first * it.second }.average()
        val x2ym = x.zip(y).map { it.first * it.first * it.second }.average()

        val sxx = x2m - xm * xm
        val sxy = xym - xm * ym
        val sxx2 = x3m - xm * x2m
        val sx2x2 = x4m - x2m * x2m
        val sx2y = x2ym - x2m * ym

        val b = (sxy * sx2x2 - sx2y * sxx2) / (sxx * sx2x2 - sxx2 * sxx2)
        val c = (sx2y * sxx - sxy * sxx2) / (sxx * sx2x2 - sxx2 * sxx2)
        val a = ym - b * xm - c * x2m

        return doubleArrayOf(a, b, c)
    }
}
import cv2 as cv
from matplotlib import pyplot as plt
from helpers import filter_yellow_and_white_lines
import numpy as np
import os
import glob
import time

### --- Definition of global variables ---
font = cv.FONT_HERSHEY_SIMPLEX

cwd = os.path.dirname(os.path.realpath(__file__)) # get correct cwd
os.chdir(cwd)

old_left_w, old_right_w = [], []

global old_frame_shapes
alpha = 0.25 # opacity of overlayed polygon

# Code was used from opencv.org/4.5.3/dc/dbb/tutorial_py_calibration.html

mtx = np.array([[1.15694047e+03, 0.00000000e+00, 6.65948821e+02],
       [0.00000000e+00, 1.15213880e+03, 3.88784788e+02],
       [0.00000000e+00, 0.00000000e+00, 1.00000000e+00]])
dist = np.array([[-2.37638058e-01, -8.54041696e-02, -7.90999653e-04,
        -1.15882218e-04,  1.05725981e-01]])

### --- Camera Calibration ---

if True:
    print("calibrating Camera", end="") # Dynamic progress bar
    st = time.time()

    # termination criteria
    criteria = (cv.TERM_CRITERIA_EPS + cv.TERM_CRITERIA_MAX_ITER, 30, 0.001)
    # prepare object points, like (0,0,0), (1,0,0), (2,0,0) ....,(6,5,0)

    cols = 9
    rows = 6

    objp = np.zeros((rows*cols,3), np.float32)
    objp[:,:2] = np.mgrid[0:rows,0:cols].T.reshape(-1,2)
    # Arrays to store object points and image points from all the images.
    objpoints = [] # 3d point in real world space
    imgpoints = [] # 2d points in image plane.
    # images = glob.glob(pathname='./img/Udacity/calib/*.jpg')
    images = glob.glob('img/Udacity/calib/*.jpg')
    shape = (0,0)
    for fname in images:
        img = cv.imread(fname)
        gray = cv.cvtColor(img, cv.COLOR_BGR2GRAY)
        shape = gray.shape[::-1]
        # Find the chess board corners
        ret, corners = cv.findChessboardCorners(gray, (rows,cols), None)
        # If found, add object points, image points (after refining them)
        if ret == True:
            objpoints.append(objp)
            corners2 = cv.cornerSubPix(gray,corners, (11,11), (-1,-1), criteria)
            imgpoints.append(corners2)
        print(".", end="")

    # Return Camera Matrix and distribution used for calibrating and undistorting camera image
    ret, mtx, dist, rvecs, tvecs = cv.calibrateCamera(objpoints, imgpoints, shape, None, None)
    print("Done! ", end="")

    et = time.time()
    mtime = round(et - st, 2)
    print(mtime, "s")



### --- Start Image Processing --- 


## video capture was implemented using code from https://www.geeksforgeeks.org/python-play-a-video-using-opencv/
video_name = 'project_video.mp4'
cap = cv.VideoCapture('img/Udacity/' + video_name)

frame_count = int(cap.get(cv.CAP_PROP_FRAME_COUNT))
current_frame = 0

# Check if camera opened successfully
if (cap.isOpened()== False):
    print("Error opening video file")
    exit()
else:
    # define parameters and video file to write result to 
    v_width  = int(cap.get(cv.CAP_PROP_FRAME_WIDTH))
    v_height = int(cap.get(cv.CAP_PROP_FRAME_HEIGHT))
    fps = cap.get(cv.CAP_PROP_FPS)
    print("Video size: ", v_width, "x", v_height)
    fourcc = cv.VideoWriter_fourcc(*'H264')
    result_video = cv.VideoWriter('./results/result_video.mp4', fourcc, fps, (v_width, v_height))

# Read until video is completed
while(cap.isOpened()):

    # Start timer
    total_start = time.time()

    # Load video frame
    ret, img1 = cap.read()

    if ret == True:
        current_frame += 1
        
        ### --- Transform Image ---


        # Define width and Height of original image
        h,  w = img1.shape[:2]

        # Undistort image using predefined Camera Matrix
        img1 = cv.undistort(img1, mtx, dist, None, mtx)

        # Define perspektive transformation corners
        src = np.float32(((560, 450), (720, 450), (200, 700), (1200, 700)))
        dst = np.float32(((0, 0), (w, 0), (0, h), (w, h)))

        # Apply perspective transformation
        M = cv.getPerspectiveTransform(src,dst)
        img1_warp = cv.warpPerspective(img1,M,(img1.shape[1], img1.shape[0]))

        # Apply filters to find lines
        filtered_lines = filter_yellow_and_white_lines(img1_warp)

        # define width and height of transformed image
        w = len(filtered_lines[1])
        h = len(filtered_lines)

        # Split image and filter for single points
        left_half = filtered_lines[:,0:int(w/2)]
        right_half = filtered_lines[:,int(w/2):w]

        kernel = np.array([[1,0,-1],[1,0,-1],[1,0,-1]],np.float32)
        left_half = cv.filter2D(left_half,-1,kernel)
        kernel = np.array([[-1,0,1],[-1,0,1],[-1,0,1]],np.float32)
        right_half = cv.filter2D(right_half,-1,kernel)


        ### --- Get points of pixels ---


        # Get Array of point coordinates from image
        left_y, left_x = np.where(left_half == np.uint32(255))
        right_y, right_x = np.where(right_half == np.uint32(255))


        ### --- get polynomial functions ---


        # Determine Polynomial Function representing the white pixel for each half

        def get_poly(x_n, y_n):
            """
            get_poly Determine the second degree polynomial function representing the points defined in the input arrays
            Uses np.polyfit

            Args:
                x_n (Array): X values of points 
                y_n (Array): Y values of points

            Returns:
                (Array, Array): Arrays containing key pints defining a second degree polynomial Function
            
            Raises: 
                ValueError: If input Array is empty
            """
            if len(x_n) == 0:
                raise ValueError

            w = np.polyfit(y_n, x_n, 2)

            xn = np.arange(0, h, 1)
            yn1 = np.poly1d(w)(xn)

            return (w, xn, yn1)

        original_overlayed = img1
        
        try:
            # Get representative Polynomial function for each half 
            # If no polynomial can be found, exit try block
            left_w, left_yn, left_xn = get_poly(left_x, left_y)
            right_w, right_yn, right_xn = get_poly(right_x, right_y)

            # Calculate end time and print time difference
            calc_end = time.time()
            mtime = round(calc_end - total_start, 3)
            print("Frame: ", current_frame, "/", frame_count, ": ", end="")
            print("Calc Time: ", mtime*1000, "ms ", end="")

            ### --- Speed up rendering by only drawing new lines if koefficients of line differ enough for last frame ---

            if len(old_left_w) == 0:
                old_left_w = left_w
                old_right_w = right_w

            t = [0.8, 0.8, 0.1] # value for tolerance when comparing new and old curve 

            # If difference between new and old polynomial is larger than allowed tolerance, use old drawn lines
            if current_frame != 1 and not np.any(abs(old_left_w-left_w) > abs(np.multiply(t,old_left_w))) or np.any(abs(old_left_w-left_w) < 0 - abs(np.multiply(t, old_left_w))):
                original_overlayed = cv.addWeighted(old_poly, alpha, original_overlayed, 1-alpha, 1)
                original_overlayed = np.where(old_poly == 0, img1, original_overlayed)

                original_overlayed[:,:,0] = np.where(old_lines[:,:,0] == 255, 255, original_overlayed[:,:,0])
                original_overlayed[:,:,1] = np.where(old_lines[:,:,0] == 255, 0, original_overlayed[:,:,1])
                original_overlayed[:,:,2] = np.where(old_lines[:,:,0] == 255, 0, original_overlayed[:,:,2])
            
            else:
                # define new polynomials to be used by tolerance checking in the next frame
                old_left_w = left_w
                old_right_w = right_w
                
                # Shift right curve to the right to display it correctly
                right_xn += np.uint32(w/2) 

                ### --- Draw shapes on polygon and transform back to original ---


                # init black image for polygon
                warp_poly = np.zeros((h, w, 3), np.uint8)
                #init black image for lines
                warp_lines = np.zeros((h, w, 3), np.uint8)

                # Create Stack out of points for each line
                left_curve_stack = np.stack((left_xn, left_yn), axis=-1)
                right_curve_stack = np.stack((right_xn, right_yn), axis=-1)

                # create array containing both lines
                pts = np.array([left_curve_stack, right_curve_stack[::-1]], np.int32)
                pts = pts.reshape((-1,1,2))

                # fill space between lines with (green) polygon
                cv.fillPoly(warp_poly, [pts], (0,255,0))

                # Draw both lines in image (blue)
                cv.polylines(warp_lines, pts, isClosed = True, color = (255, 0, 0), thickness = 60)

                # transform poylgon back to original proportions
                M = cv.getPerspectiveTransform(dst,src)
                transform_back = cv.warpPerspective(warp_poly,M,(w, h))
                old_poly = transform_back

                # overlay polygon with limited opacity onto road
                original_overlayed = cv.addWeighted(transform_back, alpha, original_overlayed, 1-alpha, 1)
                original_overlayed = np.where(transform_back == 0, img1, original_overlayed)

                # transform lines back to original proportions
                transform_back = cv.warpPerspective(warp_lines,M,(w, h))
                old_lines = transform_back

                # Overlay lines on full opacity on road
                original_overlayed[:,:,0] = np.where(transform_back[:,:,0] == 255, 255, original_overlayed[:,:,0])
                original_overlayed[:,:,1] = np.where(transform_back[:,:,0] == 255, 0, original_overlayed[:,:,1])
                original_overlayed[:,:,2] = np.where(transform_back[:,:,0] == 255, 0, original_overlayed[:,:,2])

        except(ValueError):
            print("No markings found! ", end="")

        finally:
            ### --- Write final image to file ---

            total_end = time.time()
            mtime = round(total_end - total_start, 3)
            # print("Total time: ", mtime*1000, "ms")

            # Calculate and display fps on frame
            fps = str(round(1 / mtime))
            cv.putText(original_overlayed, 'FPS: ' + fps, (10,h-10), font, 0.5, (255,255,255), 1)

            # Show image 
            cv.imshow('Frame', original_overlayed)
            if cv.waitKey(1) & 0xFF == ord('q'):
                print("exiting video playback...")
                break
            
            # Write frame to results video
            result_video.write(original_overlayed)

            # Calculate and print total time after displaying frame 
            total_end = time.time()
            mtime = round(total_end - total_start, 3)
            print("Total time: ", mtime*1000, "ms")
    
    else:
        # If frame can't be read (end of video), break out of loop and terminate video
        break

# Clean up 
cap.release()
result_video.release()

cv.destroyAllWindows()
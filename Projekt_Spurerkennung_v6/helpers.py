import cv2 as cv
import numpy as np

def filter_yellow_and_white_lines(img):
    """
    filter_yellow_and_white_lines filter the yellow and white lines from an image

    Args:
        img (Mat): image object

    Returns:
        Mat: image with only yellow and white lines
    """

    # convert to hls color space
    hls = cv.cvtColor(img, cv.COLOR_BGR2HLS)
    h_channel = hls[:,:,0]
    l_channel = hls[:,:,1]
    s_channel = hls[:,:,2]

    # filter white lines with lightness value
    l_thresh = (215, 255)
    white_mask = np.zeros_like(l_channel)
    white_mask[(l_channel >= l_thresh[0]) & (l_channel <= l_thresh[1])] = 1

    # filter yellow lines with saturation and hue value
    h_thresh = (17, 46)
    h_thresh_filtered = np.zeros_like(h_channel)
    h_thresh_filtered[(h_channel >= h_thresh[0]) & (h_channel <= h_thresh[1])] = 1

    s_thresh = (89, 255)
    s_thresh_filtered = np.zeros_like(s_channel)
    s_thresh_filtered[(s_channel >= s_thresh[0]) & (s_channel <= s_thresh[1])] = 1

    # combine yellow and white filter
    combined = np.zeros_like(h_channel)
    combined[(((s_thresh_filtered == 1) & (h_thresh_filtered == 1))) | white_mask ==1] = 255

    return combined

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := user

LOCAL_STATIC_JAVA_LIBRARIES := google-framework

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := AppMarket
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

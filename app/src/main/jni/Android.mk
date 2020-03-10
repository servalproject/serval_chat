LOCAL_PATH:= $(call my-dir)

SODIUM_ARCH_FOLDER := $(APP_ABI)
ifeq ($(SODIUM_ARCH_FOLDER),armeabi-v7a)
        SODIUM_ARCH_FOLDER = armv7-a
endif
ifeq ($(SODIUM_ARCH_FOLDER),arm64-v8a)
        SODIUM_ARCH_FOLDER = armv8-a
endif
ifeq ($(SODIUM_ARCH_FOLDER),x86)
        SODIUM_ARCH_FOLDER = i686
endif
ifeq ($(SODIUM_ARCH_FOLDER),x86_64)
        SODIUM_ARCH_FOLDER = westmere
endif

SODIUM_BASE := libsodium/libsodium-android-$(SODIUM_ARCH_FOLDER)
SODIUM_INCLUDE := $(LOCAL_PATH)/$(SODIUM_BASE)/include

include $(CLEAR_VARS)
LOCAL_MODULE:= sodium
LOCAL_SRC_FILES:= $(SODIUM_BASE)/lib/libsodium.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_STATIC_LIBRARIES := servaldstatic
LOCAL_C_INCLUDES += $(LOCAL_PATH)/serval-dna
LOCAL_SRC_FILES := $(LOCAL_PATH)/chat_features.c
LOCAL_MODULE := servaldaemon
LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -llog
include $(BUILD_SHARED_LIBRARY)

# Build serval library
include $(CLEAR_VARS)
include $(LOCAL_PATH)/serval-dna/Android.mk

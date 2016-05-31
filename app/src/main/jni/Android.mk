LOCAL_PATH:= $(call my-dir)

SODIUM_ARCH_FOLDER := $(TARGET_ARCH)
ifeq ($(SODIUM_ARCH_FOLDER),arm)
    SODIUM_ARCH_FOLDER = armv6
endif
ifeq ($(SODIUM_ARCH_FOLDER),arm-64)
    SODIUM_ARCH_FOLDER = armv8-a
endif
ifeq ($(SODIUM_ARCH_FOLDER),x86)
        SODIUM_ARCH_FOLDER = i686
endif
ifeq ($(SODIUM_ARCH_FOLDER),mips)
        SODIUM_ARCH_FOLDER = mips32
endif
SODIUM_BASE := libsodium/libsodium-android-$(SODIUM_ARCH_FOLDER)
SODIUM_INCLUDE := $(LOCAL_PATH)/$(SODIUM_BASE)/include

include $(CLEAR_VARS)
LOCAL_MODULE:= sodium
LOCAL_SRC_FILES:= $(SODIUM_BASE)/lib/libsodium.a
include $(PREBUILT_STATIC_LIBRARY)

# Build serval library
include $(CLEAR_VARS)
include $(LOCAL_PATH)/serval-dna/Android.mk


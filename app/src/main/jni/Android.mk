LOCAL_PATH:= $(call my-dir)

SODIUM_ARCH_FOLDER := $(TARGET_ARCH)
ifeq ($(SODIUM_ARCH_FOLDER),arm)
    SODIUM_ARCH_FOLDER = armv6
endif
ifeq ($(SODIUM_ARCH_FOLDER),arm64)
    SODIUM_ARCH_FOLDER = armv8-a
endif
ifeq ($(SODIUM_ARCH_FOLDER),x86)
        SODIUM_ARCH_FOLDER = i686
endif
ifeq ($(SODIUM_ARCH_FOLDER),x86_64)
        SODIUM_ARCH_FOLDER = westmere
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

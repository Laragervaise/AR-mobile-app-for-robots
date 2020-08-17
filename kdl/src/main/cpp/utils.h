/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

#pragma once

#include <jni.h>
#include <string>


std::string convertJString(JNIEnv* env, jstring jstr);

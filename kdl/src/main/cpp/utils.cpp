/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

#include "utils.h"


std::string convertJString(JNIEnv* env, jstring jstr)
{
	jboolean isCopy;
	const char* cstr = env->GetStringUTFChars(jstr, &isCopy);

	std::string result(cstr);

	env->ReleaseStringUTFChars(jstr, cstr);

	return result;
}

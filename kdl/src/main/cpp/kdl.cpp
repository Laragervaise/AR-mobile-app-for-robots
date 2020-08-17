/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

#include <jni.h>

#include "robot.h"
#include "utils.h"
#include <android/log.h>

#include <kdl/frames.hpp>
#include <kdl/segment.hpp>
#include <kdl/rigidbodyinertia.hpp>



extern "C" JNIEXPORT jlong JNICALL Java_ch_idiap_android_kdl_KDL_createRobot(
		JNIEnv* env, jclass cls, jstring jRootName)
{
	Robot* robot = new Robot(convertJString(env, jRootName));
	return reinterpret_cast<jlong>(robot);
}


extern "C" JNIEXPORT void JNICALL Java_ch_idiap_android_kdl_KDL_releaseRobot(
	JNIEnv* env, jclass cls, jlong robotHandle)
{
	Robot* robot = reinterpret_cast<Robot*>(robotHandle);
	delete robot;
}


extern "C" JNIEXPORT void JNICALL Java_ch_idiap_android_kdl_KDL_addLink(
	JNIEnv* env, jclass cls, jlong robotHandle,
	jstring jName, jstring jJointName, jstring jParentLinkName, jstring jJointType,
	jfloat origin_position_x, jfloat origin_position_y, jfloat origin_position_z,
	jfloat origin_rotation_x, jfloat origin_rotation_y, jfloat origin_rotation_z, jfloat origin_rotation_w,
	jfloat axis_x, jfloat axis_y, jfloat axis_z)
{
	Robot* robot = reinterpret_cast<Robot*>(robotHandle);

	std::string name = convertJString(env, jName);
	std::string jointName = convertJString(env, jJointName);
	std::string parentLinkName = convertJString(env, jParentLinkName);
	std::string jointType = convertJString(env, jJointType);
	KDL::Vector origin_position(origin_position_x, origin_position_y, origin_position_z);
	KDL::Rotation origin_rotation = KDL::Rotation::Quaternion(origin_rotation_x, origin_rotation_y, origin_rotation_z, origin_rotation_w);
	KDL::Vector axis(axis_x, axis_y, axis_z);


	KDL::Joint joint;

	if ((jointType == "revolute") || (jointType == "continuous"))
	{
		joint = KDL::Joint(jointName, origin_position, origin_rotation * axis, KDL::Joint::RotAxis);
	}
	else if (jointType == "prismatic")
	{
		joint = KDL::Joint(jointName, origin_position, origin_rotation * axis, KDL::Joint::TransAxis);
	}
	else
	{
		joint = KDL::Joint(jointName, KDL::Joint::None);
	}


	KDL::RigidBodyInertia inertia(0);
	KDL::Frame origin(origin_rotation, origin_position);

	KDL::Segment segment(name, joint, origin, inertia);

	robot->addSegment(segment, parentLinkName);
}


extern "C" JNIEXPORT jboolean JNICALL Java_ch_idiap_android_kdl_KDL_setKinematicChain(
	JNIEnv* env, jclass cls, jlong robotHandle, jstring jRoot, jstring jTip)
{
	Robot* robot = reinterpret_cast<Robot*>(robotHandle);

	return robot->init(convertJString(env, jRoot), convertJString(env, jTip));
}


extern "C" JNIEXPORT jobjectArray JNICALL Java_ch_idiap_android_kdl_KDL_getJointNames(
	JNIEnv* env, jclass cls, jlong robotHandle)
{
	Robot* robot = reinterpret_cast<Robot*>(robotHandle);

	auto jointNames = robot->getJointNames();

	jobjectArray result = (jobjectArray) env->NewObjectArray(
		jointNames.size(), env->FindClass("java/lang/String"), nullptr);

	for (int i = 0; i < jointNames.size(); ++i)
		env->SetObjectArrayElement(result, i, env->NewStringUTF(jointNames[i].c_str()));

	return result;
}


extern "C" JNIEXPORT void JNICALL Java_ch_idiap_android_kdl_KDL_computeJacobian(
	JNIEnv* env, jclass cls, jlong robotHandle, jfloatArray jPositionsArray)
{
	Robot* robot = reinterpret_cast<Robot*>(robotHandle);

    // transfer the initial joint positions to robot
	KDL::JntArray& positions = robot->getJointPositions();
	auto jointNames = robot->getJointNames();
	jfloat* jPositions = env->GetFloatArrayElements(jPositionsArray, 0);
	for (int i = 0; i < jointNames.size(); ++i)
		positions(i) = jPositions[i];
	env->ReleaseFloatArrayElements(jPositionsArray, jPositions, 0);

	robot->computeJacobian();
}


extern "C" JNIEXPORT jfloatArray JNICALL Java_ch_idiap_android_kdl_KDL_processJointPosition(
		JNIEnv* env, jclass cls, jlong robotHandle, jstring jName, jfloat jPosition)
{
	Robot* robot = reinterpret_cast<Robot*>(robotHandle);

	std::string name = convertJString(env, jName);

	const KDL::Frame frame = robot->processJointPosition(name, jPosition);

	double x, y, z, w;
	frame.M.GetQuaternion(x, y, z, w);

    jfloatArray jRotation = env->NewFloatArray(4);

    jfloat* jElements = env->GetFloatArrayElements(jRotation, 0);

    jElements[0] = (float) x;
    jElements[1] = (float) y;
    jElements[2] = (float) z;
    jElements[3] = (float) w;

    env->ReleaseFloatArrayElements(jRotation, jElements, 0);

    return jRotation;
}

extern "C" JNIEXPORT jfloatArray JNICALL Java_ch_idiap_android_kdl_KDL_forwardKinematics(
        JNIEnv* env, jclass cls, jlong robotHandle, jfloatArray jPositionsArray)
{
    Robot* robot = reinterpret_cast<Robot*>(robotHandle);

    // transfer the initial joint positions to robot
    auto jointNames = robot->getJointNames();
    KDL::JntArray& positions = robot->getJointPositions();
    jfloat* jPositions = env->GetFloatArrayElements(jPositionsArray, 0);
    for (int i = 0; i < jointNames.size(); ++i)
        positions(i) = jPositions[i];
    env->ReleaseFloatArrayElements(jPositionsArray, jPositions, 0);

    // compute the forward kinematics
	KDL::Frame& cartPos = robot->forwardKinematics();

	//convert the output (we only need to keep the positions)
	jfloatArray result = env->NewFloatArray(3);
	jfloat* jCartPos = env->GetFloatArrayElements(result, 0);
	jCartPos[0] = cartPos.p.x();
	jCartPos[1] = cartPos.p.y();
	jCartPos[2] = cartPos.p.z();
    env->ReleaseFloatArrayElements(result, jCartPos, 0);

    return result;
}

extern "C" JNIEXPORT jfloatArray JNICALL Java_ch_idiap_android_kdl_KDL_inverseKinematics(
        JNIEnv* env, jclass cls, jlong robotHandle, jfloatArray jPositionsArray, jfloatArray jgoalPos, jfloatArray jgoalOrient)
{
    Robot* robot = reinterpret_cast<Robot*>(robotHandle);

	auto jointNames = robot->getJointNames();

    // transfer the initial joint positions to robot
    /*KDL::JntArray& positions = robot->getJointPositions();
    jfloat* jPositions = env->GetFloatArrayElements(jPositionsArray, 0);
    for (int i = 0; i < jointNames.size(); ++i)
        positions(i) = jPositions[i];
    env->ReleaseFloatArrayElements(jPositionsArray, jPositions, 0);*/

    // transfer the goal position in a Vector variable
    jfloat* goalPosFloat = env->GetFloatArrayElements(jgoalPos, 0);
    KDL::Vector* goalPosVector;
    goalPosVector->x(goalPosFloat[0]);
    goalPosVector->y(goalPosFloat[1]);
    goalPosVector->z(goalPosFloat[2]);
    env->ReleaseFloatArrayElements(jgoalPos, goalPosFloat, 0);

	// transfer the goal orientation in a Rotation variable
	jfloat* goalOrientFloat = env->GetFloatArrayElements(jgoalOrient, 0);
	KDL::Rotation goalOrientRot = KDL::Rotation::Quaternion((double)goalOrientFloat[0],
			(double)goalOrientFloat[1], (double)goalOrientFloat[2],(double) goalOrientFloat[3]);
	env->ReleaseFloatArrayElements(jgoalOrient, goalOrientFloat, 0);

	// transfer the goal position and orientation in a Frame variable
    KDL::Frame* goalFrame = new KDL::Frame(goalOrientRot, *goalPosVector);

    //compute the inverse kinematics
	KDL::JntArray jointGoal = robot->inverseKinematics(*goalFrame); //&

    //convert the output
    jfloatArray result = env->NewFloatArray(jointNames.size());
    jfloat* jGoal = env->GetFloatArrayElements(result, 0);
    for (int i = 0; i < jointNames.size(); ++i)
        jGoal[i] = jointGoal(i);
	env->ReleaseFloatArrayElements(result, jGoal, 0);

    return result;
}

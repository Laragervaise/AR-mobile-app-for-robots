/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

#pragma once

#include <string>
#include <vector>
#include <kdl/tree.hpp>
#include <kdl/chain.hpp>
#include <kdl/segment.hpp>
#include <kdl/chainjnttojacsolver.hpp>
#include <kdl/chainiksolverpos_lma.hpp>
#include <kdl/chainfksolverpos_recursive.hpp>
#include <android/log.h>

class Robot
{
public:
	Robot(const std::string& rootName);
	~Robot();


public:
	void addSegment(const KDL::Segment& segment, const std::string& parentLinkName);

	bool init(const std::string& root, const std::string& tip);

	void computeJacobian();

	KDL::Frame processJointPosition(const std::string& name, float position);

	/**
	 * Compute the forward kinematics
	 * @return the end-effector 3D position
	 */
	KDL::Frame& forwardKinematics();

	/**
	 * Compute the inverse kinematics
	 * @param the goal position and orientation for the end-effector
	 * @return an array of all the joint angles in the goal position
	 */
	KDL::JntArray inverseKinematics(KDL::Frame goal); //&

	inline const std::vector<std::string>& getJointNames() const
	{
		return jointNames;
	}

	inline KDL::JntArray& getJointPositions()
	{
		return positions;
	}

private:
	KDL::Tree tree;
	KDL::Chain chain;
	KDL::Jacobian jacobian;
	KDL::ChainJntToJacSolver* jacobianSolver;
	std::vector<std::string> jointNames;
	KDL::JntArray positions;

	KDL::ChainFkSolverPos_recursive* fkSolver;
	KDL::Frame cartPos; // to store the result of the end-effector position

	KDL::ChainIkSolverPos_LMA* ikSolver;
	KDL::JntArray jointPosGoal; // to store the result of the joints positions

	// Frame = vector double[3] + rotation double[9]
	// JntArray is like a floatArray
};

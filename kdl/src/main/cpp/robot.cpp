/*
 * Copyright (C) 2020 Idiap Research Institute
 *
 * Authors:
 *   philip.abbet@idiap.ch (Philip Abbet)
 */

#include "robot.h"
#include <algorithm>


Robot::Robot(const std::string& rootName)
: tree(rootName), jacobianSolver(nullptr)
{
}


Robot::~Robot()
{
	delete jacobianSolver;
	delete fkSolver;
	delete ikSolver;
}


void Robot::addSegment(const KDL::Segment& segment, const std::string& parentLinkName)
{
	tree.addSegment(segment, parentLinkName);
}


bool Robot::init(const std::string& root, const std::string& tip)
{
	if (!tree.getChain(root, tip, chain))
		return false;

	jacobian = KDL::Jacobian(chain.getNrOfJoints());
	positions.resize(chain.getNrOfJoints());
    jointPosGoal.resize(chain.getNrOfJoints());

    // solvers
	jacobianSolver = new KDL::ChainJntToJacSolver(chain);
	ikSolver = new KDL::ChainIkSolverPos_LMA(chain);
	fkSolver = new KDL::ChainFkSolverPos_recursive(chain);

	for (int i = 0; i < chain.getNrOfSegments(); ++i) {
		KDL::Segment segment = chain.getSegment(i);
		KDL::Joint joint = segment.getJoint();

		if (joint.getType() == KDL::Joint::None)
			continue;

		if (std::find(jointNames.begin(), jointNames.end(), segment.getJoint().getName()) == jointNames.end())
			jointNames.push_back(segment.getJoint().getName());
	}

	return true;
}


void Robot::computeJacobian()
{
	jacobianSolver->JntToJac(positions, jacobian);
}


KDL::Frame Robot::processJointPosition(const std::string& name, float position)
{
	for (auto segment : chain.segments)
	{
		if (segment.getJoint().getName() == name)
			return segment.pose(position);
	}

	return KDL::Frame();
}


KDL::Frame& Robot::forwardKinematics() //&
{
	fkSolver->JntToCart(positions, cartPos);
	return cartPos;
}

KDL::JntArray Robot::inverseKinematics(KDL::Frame goal) //&
{
	ikSolver->CartToJnt(positions, goal, jointPosGoal);
	return jointPosGoal;
}
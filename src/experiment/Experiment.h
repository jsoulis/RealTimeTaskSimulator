#ifndef __EXPERIMENT__
#define __EXPERIMENT__

#include <string>

#include "../container/Param.h"
#include "../dataproc/ExperimentLogger.h"
#include "../ops/TaskSetParallelizer.h"
#include "../../tools/CRand.h"
#include "../../tools/FileIO.h"

class Experiment
{
protected:
	Param *pr;
	//ExperimentLogger *el;
	CRand *cr;
	TaskSetParallelizer *tsp;
	std::string expName;
	int iter;
	double utilizationInc;
	int midResult;
	int init(std::ifstream &file);
	int loadEnvironment(std::ifstream &file);
public:
	Experiment();
	~Experiment();
	Experiment(std::ifstream &file);
	int set();
	int run();
	int output();
};

#endif

#ifndef __GENERATOR__
#define __GENERATOR__

#include <iostream>
#include <fstream>
#include <stdlib.h>

#include "../container/Task.h"
#include "../container/TaskSet.h"
#include "../../tools/FileIO.h"
#include "../../tools/CRand.h"

class Generator
{
protected:
	double numTask;
	double minPeriod;
	double maxPeriod;
	double minDeadline;
	double maxDeadline;
	double minExecTime;
	double maxExecTime;
	CRand cr;
	int init();

public:
	Generator();
	Generator(int seed);

	int loadConfig(std::ifstream &file);
	int saveConfig();
	
	Task nextTask();
	TaskSet nextTaskSet();
};

#endif

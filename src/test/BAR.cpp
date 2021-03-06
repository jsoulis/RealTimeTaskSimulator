#include "BAR.h"

BAR::BAR()
{
	
}

BAR::BAR(Param *paramExt)
{
	pr = paramExt;
}

BAR::~BAR()
{
	
}

std::vector<double> BAR::getKMaxInterferingExecTime(TaskSet &ts, int k)
{
	std::vector<double> execTimes;
	for(int i = 0; i < ts.count(); i++) {
		execTimes.push_back(ts.getTask(i).getExecTime());
	}
	std::vector<double> ret = PMath::kMax(execTimes, k);

	return ret;
}

std::vector<double> BAR::calcExtendedIntervalBound(TaskSet &ts)
{
	std::vector<double> extendedIntervalList;
	std::vector<double> maxExecTime = getKMaxInterferingExecTime(ts, pr->getNProc() - 1);

	// AkBase = [Csum + sum {(Ti - Di) * Ui}]

	// Csum
	double csum = 0.0;
	for(unsigned int i = 0; i < maxExecTime.size(); i++) {
		csum += maxExecTime[i];
	}

	double AkBase = csum;
	// sum {(Ti - Di) * Ui}
	for(int i = 0; i < ts.count(); i++) {
		Task t = ts.getTask(i);
		AkBase += (t.getPeriod() - t.getDeadline()) * TaskUtil::calcUtilization(t);
	}

	// utot 
	double utot = TaskSetUtil::sumUtilization(ts);

	// => Ak = [AkBase + Dk * Utot - m * Dk + m * Ck ] / [m - Utot]
	for(int i = 0; i < ts.count(); i++) {
		Task t = ts.getTask(i);
		double nProc = pr->getNProc();
		double AkRemainder = t.getDeadline() * (utot - nProc) +  nProc * t.getExecTime();
		AkRemainder = (AkBase + AkRemainder) / (nProc - utot);
		extendedIntervalList.push_back(AkRemainder);
	}

	return extendedIntervalList;
}

double BAR::calcNCInterference(TaskSet &ts, int baseTaskIndex, int interTaskIndex, double extendedInterval)
{
	Task baseTask = ts.getTask(baseTaskIndex);
	Task interTask = ts.getTask(interTaskIndex);

	double dbf = TaskUtil::calcDemandOverInterval(interTask, extendedInterval + baseTask.getDeadline());
	double ret = 0.0;
	// case i != k
	if(baseTaskIndex != interTaskIndex) {
		// min(DBF(ti, ak + dk), ak + dk - ck)
		ret = std::min(dbf, extendedInterval + baseTask.getDeadline() - baseTask.getExecTime());

	// case i == k
	} else {
		// min(DBF(ti, ak + dk) - ck, ak)	(i == k)
		ret = std::min(dbf - interTask.getExecTime(), extendedInterval);
	}

	return ret;
}

double BAR::calcCIInterference(TaskSet &ts, int baseTaskIndex, int interTaskIndex, double extendedInterval)
{
	Task baseTask = ts.getTask(baseTaskIndex);
	Task interTask = ts.getTask(interTaskIndex);

	
	
	// min(Ci, t mod Ti)
	// t mod Ti
	double ret = std::fmod(extendedInterval + baseTask.getDeadline(), interTask.getPeriod());	
	ret = std::min(ret, interTask.getExecTime());

	// |t / Ti| * Ci 
	ret += std::floor((extendedInterval + baseTask.getDeadline()) / interTask.getPeriod()) * interTask.getExecTime();

	// case i != k
	if(baseTaskIndex != interTaskIndex) {
		// min(DBF'(ti, Ak + Dk), Ak + Dk - Ck)
		ret = std::min(ret, extendedInterval + baseTask.getDeadline() - baseTask.getExecTime());
	// case i == k
	} else {
		// min(DBF'(ti, Ak + Dk) - Ck, Ak)
		ret = std::min(ret - baseTask.getExecTime(), extendedInterval);
	}

	return ret;
}

int BAR::calcIDiff() 
{
	iDiff.clear();
	for(unsigned int i = 0; i < iNC.size(); i++) {
		iDiff.push_back(iCI[i] - iNC[i]);
		if(iDiff[i] < 0) {
			std::cout<<"iDiff calculation error"<<std::endl;
		}
	}
	return 1;
}

int BAR::debugPrintIDiff()
{
	for(unsigned int i = 0; i < iNC.size(); i++) {
		std::cout << i << ", [" << iNC[i] << ", " << iCI[i] << ", " << iDiff[i] << "]" << std::endl;
	}
	return 1;
}

bool BAR::isSchedulablePrint(TaskSet &ts)
{
	std::cout<<"BAR isSchedulablePrint"<<std::endl;

	// Ak bound
	std::vector<double> extIntervalBoundList = calcExtendedIntervalBound(ts);

	for(int baseTaskIndex = 0; baseTaskIndex < ts.count(); baseTaskIndex++) {
		// Ak <= 0, don't need to check
		if(extIntervalBoundList[baseTaskIndex] <= 0.0) {
			continue;
		}
		std::cout<<"baseTaskIndex : "<<baseTaskIndex<<std::endl;
		// RHS m(Ak + Dk - Ck)
		double rhs = ts.getTask(baseTaskIndex).getDeadline() - ts.getTask(baseTaskIndex).getExecTime();
		
		// iterate with Ak
		double extInterval = 0.0;
		while(extInterval < extIntervalBoundList[baseTaskIndex]) {

			iNC.clear();
			for(int interTaskIndex = 0; interTaskIndex < ts.count(); interTaskIndex++) {
				// non carry-in
				iNC.push_back(calcNCInterference(ts, baseTaskIndex, interTaskIndex, extInterval));
			}

			iCI.clear();
			for(int interTaskIndex = 0; interTaskIndex < ts.count(); interTaskIndex++) {
				// carry-in
				iCI.push_back(calcCIInterference(ts, baseTaskIndex, interTaskIndex, extInterval));
			}
			
			// IDiff
			calcIDiff();

			// Sum I
			double isum = 0.0;
			for(unsigned int i = 0; i < iNC.size(); i++) {
				isum += iNC[i];
			}

			// find m - 1 largest carry-in
			std::vector<double> iKMaxCI = PMath::kMax(iDiff, pr->getNProc() - 1);
			for(unsigned int i = 0; i < iKMaxCI.size(); i++) {
				isum += iKMaxCI[i];
			}
			if(rhs + extInterval != 0) {
				std::cout<<extInterval<<", "<<isum / (rhs + extInterval);
			} else {
				std::cout<<extInterval<<", -1";;
			}
			
			// unschedule condition
			if(isum > pr->getNProc() * (rhs + extInterval)) {
				std::cout<<" ****";
			}
			std::cout<<std::endl;


			// next A (for now)
			// can be checked everytime DBF changes
			extInterval += 1.0;
		}
	}
	return true;
}

bool BAR::isSchedulable(TaskSet &ts)
{
	// Ak bound
	std::vector<double> extIntervalBoundList = calcExtendedIntervalBound(ts);

	// Trivial Condition
	double tsUtilSum = TaskSetUtil::sumUtilization(ts);
	if(tsUtilSum <= 1.0) {
		return true;
	}
	if(tsUtilSum >= pr->getNProc()) {
		return false;
	}

	// Don't handle Dk > Ck case
	for(int baseTaskIndex = 0; baseTaskIndex < ts.count(); baseTaskIndex++) {
		if(ts.getTask(baseTaskIndex).getExecTime() > ts.getTask(baseTaskIndex).getDeadline()) {
			return false;
		}
	}
	
	for(int baseTaskIndex = 0; baseTaskIndex < ts.count(); baseTaskIndex++) {		
		// Ak <= 0, don't need to check
		if(extIntervalBoundList[baseTaskIndex] <= 0.0) {
			continue;
		}

		// RHS m(Ak + Dk - Ck)
		double rhs = ts.getTask(baseTaskIndex).getDeadline() - ts.getTask(baseTaskIndex).getExecTime();
		
		// iterate with Ak
		double extInterval = 0.0;
		while(extInterval < extIntervalBoundList[baseTaskIndex]) {

			iNC.clear();
			for(int interTaskIndex = 0; interTaskIndex < ts.count(); interTaskIndex++) {
				// non carry-in
				iNC.push_back(calcNCInterference(ts, baseTaskIndex, interTaskIndex, extInterval));
			}

			iCI.clear();
			for(int interTaskIndex = 0; interTaskIndex < ts.count(); interTaskIndex++) {
				// carry-in
				iCI.push_back(calcCIInterference(ts, baseTaskIndex, interTaskIndex, extInterval));
			}
			
			// IDiff
			calcIDiff();

			// Sum I
			double isum = 0.0;
			for(unsigned int i = 0; i < iNC.size(); i++) {
				isum += iNC[i];
			}

			// find m - 1 largest carry-in
			std::vector<double> iKMaxCI = PMath::kMax(iDiff, pr->getNProc() - 1);
			for(unsigned int i = 0; i < iKMaxCI.size(); i++) {
				isum += iKMaxCI[i];
			}

			// unschedule condition
			if(isum > pr->getNProc() * (rhs + extInterval)) {
				return false;
			}

			// next A (for now)
			// can be checked everytime DBF changes
			extInterval += 1.0;
		}
	}

	return true;
}
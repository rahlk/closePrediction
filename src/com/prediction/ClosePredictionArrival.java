package com.prediction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import com.data.TestProject;
import com.data.TestReport;
import com.dataProcess.TestProjectReader;
import com.performance.PerformanceEvaluation;

public class ClosePredictionArrival extends ClosePrediction {
	//based on a constant number of reports, rather than a constant time interval
	@Override
	public Double[] predictCloseTime(TestProject project, Integer[] thresList) {
		// TODO Auto-generated method stub
		Integer constReportNumlThres = thresList[0];
		Boolean isTest = false;
		if ( thresList[1] == 1 )
			isTest = true;
		
		ArrayList<TestReport> reportList = project.getTestReportsInProj();
		
		HashMap<Integer, String> noDupReportList = new HashMap<Integer, String>();
		HashSet<String> noDupReport = new HashSet<String>();
		for ( int i =0; i < reportList.size(); i++ ) {
			TestReport report = reportList.get(i);
			String tag = report.getBugTag();
			String dupTag = report.getDupTag();
			
			if ( tag.equals( "���ͨ��") && !noDupReport.contains( dupTag )) {
				noDupReportList.put( i, "bug");
				noDupReport.add( dupTag );
			}else {
				noDupReportList.put( i, "no");
			}
		}
		//System.out.println( "===================== " + noDupReport.size() );
		TreeMap<Integer, Integer> timeDistList = new TreeMap<Integer, Integer>();
		/*
		 * based on the time interval
		Date beginTime = reportList.get(0).getSubmitTime();
		for ( int i =0; i < reportList.size(); i++ ) {
			TestReport report = reportList.get( i );
			String finalTag = noDupReportList.get( i );
			
			Date submitTime = report.getSubmitTime();
			int minutes = (int) (( submitTime.getTime() - beginTime.getTime()) / (1000*60)); 
			int interval = minutes / timeIntervalThres;

			int count = 0;
			if ( finalTag.equals( "bug")) {
				count = 1;			
			}
			if ( timeDistList.containsKey( interval )) {
				count += timeDistList.get( interval );
			}	
			timeDistList.put( interval, count );
		}
		System.out.println( timeDistList.toString() );
		
		//��ʱ����Щ��Ŀ��ʱ����̫���ˣ�һ��Сʱ��Ϊһ��time interval����ɣ��ܶ�keyû�е������{0=0, 49=1, 50=0, 62=3, 63=1, 86=0, 87=2, 90=1}
		//����Ĵ����ǽ�key�����������У���û�е�key��ȫ������ {0=0, 1=1, 2=0, 3=3, 4=1, 5=0, 6=2, 7=1}
		//���ڹ̶�report����ģ��������������
		TreeMap<Integer, Integer> newTimeDistList = new TreeMap<Integer, Integer>();
		int intervalIndex = 0;
		for ( Integer interval : timeDistList.keySet() ) {
			Integer number = timeDistList.get( interval);
			newTimeDistList.put( intervalIndex, number );
			intervalIndex++;
		}
		timeDistList = newTimeDistList;
		*/
		//based on the contant number of bugs
		for ( int i =0; i < reportList.size(); i++ ) {
			String finalTag = noDupReportList.get( i );
			
			int interval = i / constReportNumlThres;
			int count = 0;
			if ( finalTag.equals( "bug")) {
				count = 1;			
			}
			if ( timeDistList.containsKey( interval )) {
				count += timeDistList.get( interval );
			}	
			timeDistList.put( interval, count );
		}
		System.out.println( timeDistList.toString() );
		
		//find the i point when the i+1 point is smaller than i
		int priorBugNum = 0, maxIndex = -1, maxBugNum = -1;
		for ( Integer interval : timeDistList.keySet() ) {
			Integer number = timeDistList.get( interval );
			if ( priorBugNum <= number) {
				priorBugNum = number;
				continue;
			}else {
				maxIndex = interval;
				maxBugNum = priorBugNum;
				break;
			}
		}
		//trick
		/*
		 * �����ĵ㣨�����ͬ�Ļ�������һ���½��ĵ㣩��Ϊ�յ㣻
		 * ����Ĳ������ҵ���һ���յ㣻�������ж�����и��������¹յ㣬�����ͬ�Ļ����Ͳ�������
		 */
		/*
		for ( Integer interval : timeDistList.keySet() ) {
			Integer number = timeDistList.get( interval );
			if ( maxBugNum < number ) {   // here is <, the former one is <=
				maxBugNum = number;
				maxIndex = interval+1;
			}
		}
		*/
		
		System.out.print( maxIndex + " " + maxBugNum + " " );
		Integer C = maxIndex * maxIndex * 2;
		
		DecimalFormat df = new DecimalFormat("######0"); //��ɫ����ת��������
		ArrayList<Integer> totalBugList = new ArrayList<Integer>();
		
		int sumTotalBugs =0, numSum = 0;
		//��ǰ����0��ȥ��
		for ( int t = 1; t <= maxIndex; t++) {
			if ( !timeDistList.containsKey( t-1 ))
				continue;
			
			Integer ft = timeDistList.get( t-1 );
			
			double bugs = C * Math.exp( (1.0 * t*t) / C) * ft / (2*t) ;    //predicted total number of bugs
			Integer totalBugs = Integer.parseInt(df.format( bugs ));
			totalBugList.add( totalBugs );
			if ( totalBugs == 0 ) {
				continue;
			}
			
			sumTotalBugs += totalBugs;
			numSum ++;
			
			System.out.print( totalBugs + " " );
		}
		System.out.println( );
		Integer finalTotalBug = 0;
		if ( numSum != 0 )
			finalTotalBug = (int)sumTotalBugs / numSum ;
		System.out.println( "finalTotalBugs : " + finalTotalBug );
		
		if ( isTest ) {
			PerformanceEvaluation evaluation = new PerformanceEvaluation( "data/input/bugCurveStatistics.csv");
			Integer[] results = new Integer[totalBugList.size() +2];    //finalTotalBugNum, toalBugNum predicted based on f1, f2, f3... , currently the f1,f2,f3 are only for demonstration
			results[0] = finalTotalBug;
			results[1] = this.obtainEndReport(reportList, finalTotalBug);
			
			for ( int i =0; i < totalBugList.size(); i++ )
				results[i+2] = totalBugList.get( i );
			Double[] performance = evaluation.evaluatePerformanceDistribution( results, project.getProjectName() );
			return performance;
		}else {
			//call by ClosePredictionCRCMthDistribution only to obtain the predicted number of bugs
			Double[] results = { 1.0 * finalTotalBug };
			return results;
		}
	}
	
	//�ҵ�finalBugNum��Ӧ��reportList�ж�Ӧ��report����Ŀ
	public Integer obtainEndReport ( ArrayList<TestReport> reportList, Integer finalBugNum ) {
		int endReport = reportList.size();
		HashSet<String> noDupTagList = new HashSet<String>();
		for ( int i =0; i < reportList.size(); i++ ) {
			TestReport report = reportList.get(i);
			String tag = report.getBugTag();
			String dupTag = report.getDupTag();
			if ( tag.equals( "���ͨ��") ) {
				noDupTagList.add( dupTag );
			}
			if ( noDupTagList.size() == finalBugNum ) {
				endReport = i+1;
				break;
			}
		}
		return endReport;
	}
	
	@Override
	public void predictCloseTimeForProjects(String folderName, String performanceFile, Integer[] thresList) {
		// TODO Auto-generated method stub
		super.predictCloseTimeForProjects(folderName, performanceFile, thresList);
	}

	//here the project is not the whole project, but only the first K reports inspected
	public Integer totalBugNumBasedTimeDist ( TestProject project , Integer timeIntervalThres) {
		ClosePredictionArrival prediction = new ClosePredictionArrival();
		Integer[] thresList = { timeIntervalThres, 0};
		Double[] results = prediction.predictCloseTime(project, thresList);
		
		int totalBugs =  results[0].intValue();
		
		return totalBugs;
	}
	
	public static void main ( String args[] ) {
		ClosePredictionArrival prediction = new ClosePredictionArrival();
		
		int beginIndex = 27, endIndex = 27;
		for ( int i = beginIndex; i <= endIndex; i++ ) {
			Integer[] thresList = {  i, 1 };
			String tag = thresList[0].toString() + "-" + thresList[1].toString();
			prediction.predictCloseTimeForProjects(  "data/input/projects", "data/output/performance/performanceArrival-" + tag + ".csv", thresList );
		}
		
		/*
		int beginIndex = 13, endIndex = 42;
		Integer[] candPara = new Integer[endIndex-beginIndex+1];
		for ( int i = beginIndex; i <= endIndex; i++ ) {
			candPara[i-beginIndex] = i;
		}
		prediction.crossValidationForProjects( "data/input/projects", candPara, "data/output/crossPerformanceArrival", 100 );
		*/
	}
}

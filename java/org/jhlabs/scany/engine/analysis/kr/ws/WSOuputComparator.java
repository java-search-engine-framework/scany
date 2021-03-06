package org.jhlabs.scany.engine.analysis.kr.ws;

import java.util.Comparator;

import org.jhlabs.scany.engine.analysis.kr.ma.AnalysisOutput;
import org.jhlabs.scany.engine.analysis.kr.ma.PatternConstants;

public class WSOuputComparator implements Comparator<AnalysisOutput> {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(AnalysisOutput o1, AnalysisOutput o2) {
		// 길이의 역순으로 정렬한다.
		int score = o2.getScore() - o1.getScore();
		if(score != 0)
			return score;

		int len = o2.getSource().length() - o1.getSource().length();
		if(len != 0)
			return len;

		int ptn = getPtnScore(o2.getPatn()) - getPtnScore(o1.getPatn());
		if(ptn != 0)
			return ptn;

		int stem = o1.getStem().length() - o2.getStem().length();
		if(stem != 0)
			return stem;

		return 0;
	}

	private int getPtnScore(int ptn) {
		if(ptn == PatternConstants.PTN_N)
			ptn = 7;
		else if(ptn == PatternConstants.PTN_AID)
			return 50;

		return ptn;
	}

}

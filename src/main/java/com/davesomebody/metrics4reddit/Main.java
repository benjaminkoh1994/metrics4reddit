package com.davesomebody.metrics4reddit;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.TimePeriod;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class Main {
	private static final String userAgentPlatformPropertyName = "metrics4reddit.userAgent.platform";
	private static final String userAgentAppIdPropertyName = "metrics4reddit.userAgent.appId";
	private static final String userAgentVerionPropertyName = "metrics4reddit.userAgent.version";
	private static final String userAgentUserNamePropertyName = "metrics4reddit.userAgent.userName";
	public static String appIdPropertyName = "metrics4reddit.reddit.appId";
	public static String secretPropertyName = "metrics4reddit.reddit.secret";
	
	private static HashMap<String,SummaryStatistics> commenterStatistics = new HashMap<String,SummaryStatistics>();
	private static HashMap<String,SummaryStatistics> posterStatistics = new HashMap<String,SummaryStatistics>();

	
	public static int outputCount = 0;

	/**
	 * @param args
	 * @throws IOException
	 * @throws OAuthException
	 * @throws NetworkException
	 */
	public static void main(String args[]) throws IOException, NetworkException, OAuthException {
		String commentsOutputFile = args[0];
		String posterStatsOutputFile = args[1];
		String commenterStatsOutputFile = args[2];
		String userid = args[3];
		String password = args[4];
		String subreddit = args[5];
		String sortingName = args[6];
		Sorting sorting = Sorting.valueOf(sortingName);
		String timePeriodString = args[7];
		TimePeriod timePeriod = TimePeriod.valueOf(timePeriodString);
		// Sorting.CONTROVERSIAL, Sorting.GILDED, Sorting.HOT, Sorting.NEW,
		// Sorting.RISING, Sorting.TOP
		// TimePeriod.DAY, TimePeriod.HOUR, TimePeriod.MONTH, TimePeriod.WEEK,
		// TimePeriod.YEAR
		
		System.out.println("Looking for metrics4reddit.properties in classpath");
		InputStream propertiesStream = Main.class.getClassLoader().getResourceAsStream("metrics4reddit.properties");
		Properties myProperties = new Properties();
		myProperties.load(propertiesStream);
		propertiesStream.close();
		
		String appId = myProperties.getProperty(appIdPropertyName);
		String secret = myProperties.getProperty(secretPropertyName);
		String userAgentPlatform = myProperties.getProperty(userAgentPlatformPropertyName);
		String userAgentAppId = myProperties.getProperty(userAgentAppIdPropertyName);
		String userAgentVersion = myProperties.getProperty(userAgentVerionPropertyName);
		String userAgentUserName = myProperties.getProperty(userAgentUserNamePropertyName);
		
		
		FileWriter commentsFWriter = new FileWriter(commentsOutputFile);
		PrintWriter commentWriter = new PrintWriter(commentsFWriter);

		try {
			UserAgent userAgent = UserAgent.of(userAgentPlatform, userAgentAppId, userAgentVersion,
					userAgentUserName);
			RedditClient reddit = new RedditClient(userAgent);
			OAuthHelper oauthHelper = reddit.getOAuthHelper();
			Credentials creds = Credentials.script(userid, password, appId, secret);
			OAuthData auth = oauthHelper.easyAuth(creds);
			reddit.authenticate(auth);

			commentWriter.println(
					"c_id, c_parent_id, c_author, c_score, c_created, s_author, s_title, s_score, s_created, s_commentCount, c_permalink");

			SubredditPaginator paginator = new SubredditPaginator(reddit, subreddit);
			paginator.setSorting(sorting);
			paginator.setTimePeriod(timePeriod);
			paginator.setLimit(100);
			for (Submission link : paginator.next()) {
				int which = outputCount++ % 2;
				
				if(which == 0) System.out.print("+");
				else System.out.print("-");
				
				Submission fullSubmission = reddit.getSubmission(link.getId());
				String author = fullSubmission.getAuthor();
				Date created = fullSubmission.getCreated();
				Integer commentCount = fullSubmission.getCommentCount();
				Integer score = fullSubmission.getScore();
				String title = fullSubmission.getTitle();
				String permalink = fullSubmission.getPermalink();
				
				SummaryStatistics posterStats = posterStatistics.get(author);
				if(posterStats == null){
					posterStats = new SummaryStatistics();
					posterStatistics.put(author, posterStats);
				}
				posterStats.addValue(score);
				
				String submissionDetails = String.format("%s,\"%s\", %d, \"%s\", %d, %s", author, title, score, created.toString(),
						commentCount, permalink);

				CommentNode submissionRootComment = fullSubmission.getComments();

				EnumerateComments(commentWriter, submissionDetails, reddit, submissionRootComment, "");
			}

		} finally {
			commentWriter.close();
		}
		
		FileWriter commenterStatsFWriter = new FileWriter(commenterStatsOutputFile);
		PrintWriter commenterStatsWriter = new PrintWriter(commenterStatsFWriter);
		
		try {
			commenterStatsWriter.printf("userName, count, min, max, mean, sum, stdDev\n");

			for(Entry<String, SummaryStatistics> statPair : commenterStatistics.entrySet()){
				String userName = statPair.getKey();
				SummaryStatistics userStats = statPair.getValue();
				
				commenterStatsWriter.printf("%s, %d, %f, %f, %f, %f, %f\n", userName, userStats.getN(), userStats.getMin(), userStats.getMax(), userStats.getMean(), userStats.getSum(), userStats.getStandardDeviation());
			}
		} finally {
			commenterStatsWriter.close();
		}

		FileWriter posterStatsFWriter = new FileWriter(posterStatsOutputFile);
		PrintWriter posterStatsWriter = new PrintWriter(posterStatsFWriter);
		
		try {
			posterStatsWriter.printf("poster, count, min, max, mean, sum, stdDev\n");

			for(Entry<String, SummaryStatistics> statPair : posterStatistics.entrySet()){
				String userName = statPair.getKey();
				SummaryStatistics userStats = statPair.getValue();
				
				posterStatsWriter.printf("%s, %d, %f, %f, %f, %f, %f\n", userName, userStats.getN(), userStats.getMin(), userStats.getMax(), userStats.getMean(), userStats.getSum(), userStats.getStandardDeviation());
			}
		} finally {
			posterStatsWriter.close();
		}
		
	}

	private static void EnumerateComments(PrintWriter writer, String submissionDetails, RedditClient reddit,
			CommentNode comment, String parentId) {
		List<CommentNode> subcommentNodes = comment.getChildren();

		for (CommentNode subcommentNode : subcommentNodes) {
			subcommentNode.loadFully(reddit);

			Comment subcomment = subcommentNode.getComment();
			String commentId = subcomment.getId();
			String commentAuthor = subcomment.getAuthor();
			Integer commentScore = subcomment.getScore();
			Date commentCreated = subcomment.getCreated();
			SummaryStatistics authorStatistics = commenterStatistics.get(commentAuthor);
			if(authorStatistics == null){
				authorStatistics = new SummaryStatistics();
				commenterStatistics.put(commentAuthor, authorStatistics);
			}

			authorStatistics.addValue(commentScore);
			
			writer.println(String.format("%s, %s, %s, %d, \"%s\", %s%s", commentId, parentId, commentAuthor, commentScore,
					commentCreated.toString(), submissionDetails, commentId));

			EnumerateComments(writer, submissionDetails, reddit, subcommentNode, commentId);
		}
	}
}

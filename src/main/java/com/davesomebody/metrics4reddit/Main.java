package com.davesomebody.metrics4reddit;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
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

public class Main {
	private static final String userAgentPlatformPropertyName = "metrics4reddit.userAgent.platform";
	private static final String userAgentAppIdPropertyName = "metrics4reddit.userAgent.appId";
	private static final String userAgentVerionPropertyName = "metrics4reddit.userAgent.version";
	private static final String userAgentUserNamePropertyName = "metrics4reddit.userAgent.userName";
	public static String appIdPropertyName = "metrics4reddit.reddit.appId";
	public static String secretPropertyName = "metrics4reddit.reddit.secret";

	
	public static int outputCount = 0;

	/**
	 * @param args
	 * @throws IOException
	 * @throws OAuthException
	 * @throws NetworkException
	 */
	public static void main(String args[]) throws IOException, NetworkException, OAuthException {
		String outputfile = args[0];
		String userid = args[1];
		String password = args[2];
		String subreddit = args[3];
		String sortingName = args[4];
		Sorting sorting = Sorting.valueOf(sortingName);
		String timePeriodString = args[5];
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
		
		FileWriter fWriter = new FileWriter(outputfile);
		PrintWriter writer = new PrintWriter(fWriter);

		try {
			UserAgent userAgent = UserAgent.of(userAgentPlatform, userAgentAppId, userAgentVersion,
					userAgentUserName);
			RedditClient reddit = new RedditClient(userAgent);
			OAuthHelper oauthHelper = reddit.getOAuthHelper();
			Credentials creds = Credentials.script(userid, password, appId, secret);
			OAuthData auth = oauthHelper.easyAuth(creds);
			reddit.authenticate(auth);

			writer.println(
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

				String submissionDetails = String.format("%s,\"%s\", %d, \"%s\", %d, %s", author, title, score, created.toString(),
						commentCount, permalink);

				CommentNode submissionRootComment = fullSubmission.getComments();

				EnumerateComments(writer, submissionDetails, reddit, submissionRootComment, "");
			}

		} finally {
			writer.close();
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

			writer.println(String.format("%s, %s, %s, %d, \"%s\", %s%s", commentId, parentId, commentAuthor, commentScore,
					commentCreated.toString(), submissionDetails, commentId));

			EnumerateComments(writer, submissionDetails, reddit, subcommentNode, commentId);
		}
	}
}

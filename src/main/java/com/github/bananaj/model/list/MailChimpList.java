/**
 * @author alexanderweiss
 * @date 06.11.2015
 */
package com.github.bananaj.model.list;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.bananaj.connection.MailChimpConnection;
import com.github.bananaj.exceptions.EmailException;
import com.github.bananaj.exceptions.TransportException;
import com.github.bananaj.model.JSONParser;
import com.github.bananaj.model.SortDirection;
import com.github.bananaj.model.list.interests.Interest;
import com.github.bananaj.model.list.interests.InterestCategory;
import com.github.bananaj.model.list.member.Member;
import com.github.bananaj.model.list.member.MemberNote;
import com.github.bananaj.model.list.member.MemberStatus;
import com.github.bananaj.model.list.member.MemberTag;
import com.github.bananaj.model.list.mergefield.MergeField;
import com.github.bananaj.model.list.segment.Segment;
import com.github.bananaj.model.list.segment.SegmentOptions;
import com.github.bananaj.model.list.segment.SegmentType;
import com.github.bananaj.model.report.AbuseReport;
import com.github.bananaj.utils.DateConverter;
import com.github.bananaj.utils.EmailValidator;
import com.github.bananaj.utils.ModelIterator;


/**
 * Mailchimp list, also known as audience, is where you store and manage all of your contacts.
 * 
 * @author alexanderweiss
 *
 */
public class MailChimpList implements JSONParser {

	private String id;				// A string that uniquely identifies this list.
	private int webId;				// The ID used in the Mailchimp web application. View this list in your Mailchimp account at https://{dc}.admin.mailchimp.com/lists/members/?id={web_id}
	private String name;			// The name of the list
	private ListContact contact;	// Contact information displayed in campaign footers to comply with international spam laws
	private String permissionReminder;	// The permission reminder for the list
	private boolean useArchiveBar;	// Whether campaigns for this list use the Archive Bar in archives by default
	private ListCampaignDefaults campaignDefaults;	// Default values for campaigns created for this list
	private String notifyOnSubscribe;	// The email address to send subscribe notifications to
	private String notifyOnUnsubscribe; // The email address to send unsubscribe notifications to 
	private ZonedDateTime dateCreated;	// The date and time that this list was created
	private int listRating;			// An auto-generated activity score for the list (0-5)
	private boolean emailTypeOption;	// Whether the list supports multiple formats for emails. When set to true, subscribers can choose whether they want to receive HTML or plain-text emails. When set to false, subscribers will receive HTML emails, with a plain-text alternative backup.
	private String subscribeUrlShort;	// EepURL shortened version of this list’s subscribe form
	private String subscribeUrlLong;	// The full version of this list’s subscribe form (host will vary)
	private String beamerAddress;	// The list’s Email Beamer address
	private ListVisibility visibility;	// Whether this list is public or private (pub, prv)
	private boolean doubleOptin;	// Whether or not to require the subscriber to confirm subscription via email
	private boolean hasWelcome;		// Whether or not this list has a welcome automation connected. Welcome Automations: welcomeSeries, singleWelcome, emailFollowup
	private boolean marketingPermissions;	// Whether or not the list has marketing permissions (eg. GDPR) enabled
	//private List<?> modules;		// Any list-specific modules installed for this list.
	private ListStats stats;		// Stats for the list. Many of these are cached for at least five minutes.
	private MailChimpConnection connection;
	

	public MailChimpList() {
		
	}
	
	public MailChimpList(MailChimpConnection connection, JSONObject jsonList) {
		parse(connection, jsonList);
	}
	
	public MailChimpList(Builder b) {
        connection = b.connection;
    	name = b.name;
    	contact = b.contact;
    	permissionReminder = b.permissionReminder;
    	useArchiveBar = b.useArchiveBar;
    	campaignDefaults = b.campaignDefaults;
    	notifyOnSubscribe = b.notifyOnSubscribe;
    	notifyOnUnsubscribe = b.notifyOnUnsubscribe; 
    	emailTypeOption = b.emailTypeOption;
    	visibility = b.visibility;
    	doubleOptin = b.doubleOptin;
    	marketingPermissions = b.marketingPermissions;
	}
	
	public void parse(MailChimpConnection connection, JSONObject jsonList) {
		id = jsonList.getString("id");
		webId = jsonList.getInt("web_id");
		name = jsonList.getString("name");
		contact = new ListContact(jsonList.getJSONObject("contact"));
		permissionReminder = jsonList.getString("permission_reminder");
		useArchiveBar = jsonList.getBoolean("use_archive_bar");
		campaignDefaults = new ListCampaignDefaults(jsonList.getJSONObject("campaign_defaults"));
		notifyOnSubscribe = jsonList.getString("notify_on_subscribe");
		notifyOnUnsubscribe = jsonList.getString("notify_on_unsubscribe");
		dateCreated = DateConverter.fromISO8601(jsonList.getString("date_created"));
		listRating = jsonList.getInt("list_rating");
		emailTypeOption = jsonList.getBoolean("email_type_option");
		subscribeUrlShort = jsonList.getString("subscribe_url_short");
		subscribeUrlLong = jsonList.getString("subscribe_url_long");
		beamerAddress = jsonList.getString("beamer_address");
		visibility = ListVisibility.lookup(jsonList.getString("visibility"));
		doubleOptin = jsonList.getBoolean("double_optin");
		hasWelcome = jsonList.getBoolean("has_welcome");
		marketingPermissions = jsonList.getBoolean("marketing_permissions");
		// TODO: modules = jsonList.getJSONArray("modules");
		stats = new ListStats(jsonList.getJSONObject("stats"));
		this.connection = connection;
	}


	/**
	 * Get information about abuse reports. An abuse complaint occurs when your
	 * recipient reports an email as spam in their mail program.
	 * 
	 * @param count Number of abuse reports to return. Maximum value is 1000.
	 * @param offset Zero based offset
	 * @return List of abuse reports
	 * @throws Exception
	 */
	public List<AbuseReport> getAbuseReports(int count, int offset) throws Exception {
		if (count < 1 || count > 1000) {
			throw new InvalidParameterException("Page size must be 1-1000");
		}
		final JSONObject list = new JSONObject(connection.do_Get(new URL(connection.getListendpoint()+"/"+getId()+"/abuse-reports?count="+count+"&offset="+offset), connection.getApikey()));
		final JSONArray rptArray = list.getJSONArray("abuse_reports");
		ArrayList<AbuseReport> reports = new ArrayList<AbuseReport>(rptArray.length());
		for (int i = 0 ; i < rptArray.length();i++)
		{
			final JSONObject rptDetail = rptArray.getJSONObject(i);
			AbuseReport report = new AbuseReport(rptDetail);
			reports.add(report);
		}
		return reports;
	}
	
	/**
	 * Get abuse reports iterator. An abuse complaint occurs when your recipient
	 * reports an email as spam in their mail program.
	 * 
	 * Checked exceptions, including TransportException and JSONException, are
	 * warped in a RuntimeException to reduce the need for boilerplate code inside
	 * of lambdas.
	 * 
	 * @return AbuseReport iterator
	 */
	public Iterable<AbuseReport> getAbuseReports() {
		final String baseURL = getConnection().getListendpoint()+"/"+getId()+"/abuse-reports";
		return new ModelIterator<AbuseReport>(AbuseReport.class, baseURL, getConnection());
	}

	/**
	 * Get details about a specific abuse report. An abuse complaint occurs when
	 * your recipient reports an email as spam in their mail program.
	 * 
	 * @param reportId
	 * @return Details about a specific abuse report
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 * @throws JSONException
	 */
	public AbuseReport getAbuseReports(int reportId) throws JSONException, MalformedURLException, TransportException, URISyntaxException {
		final JSONObject report = new JSONObject(connection.do_Get(new URL(connection.getListendpoint()+"/"+getId()+"/abuse-reports/"+reportId), connection.getApikey()));
    	return new AbuseReport(report);
	}
	
	// TODO: Add support for Activity -- Get recent daily, aggregated activity stats for your list. For example, view unsubscribes, signups, total emails sent, opens, clicks, and more, for up to 180 days.
//	public List<ListActivity> getActivity() {
//		final JSONObject list = new JSONObject(connection.do_Get(new URL(connection.getListendpoint()+"/"+getId()+"/activity"), connection.getApikey()));
//		final JSONArray rptArray = list.getJSONArray("activity");
//		ArrayList<ListActivity> reports = new ArrayList<ListActivity>(rptArray.length());
//		for (int i = 0 ; i < rptArray.length();i++)
//		{
//			final JSONObject rptDetail = rptArray.getJSONObject(i);
//			ListActivity report = new ListActivity(rptDetail);
//			reports.add(report);
//		}
//		return reports;
//	}

	// TODO: Add support for Clients -- Get information about the most popular email clients for subscribers in a specific Mailchimp list.
//	public List<Clients> getActivity() {
//		final JSONObject list = new JSONObject(connection.do_Get(new URL(connection.getListendpoint()+"/"+getId()+"/clients"), connection.getApikey()));
//		final JSONArray rptArray = list.getJSONArray("clients");
//		ArrayList<Clients> reports = new ArrayList<Clients>(rptArray.length());
//		for (int i = 0 ; i < rptArray.length();i++)
//		{
//			final JSONObject rptDetail = rptArray.getJSONObject(i);
//			Clients report = new Clients(rptDetail);
//			reports.add(report);
//		}
//		return reports;
//	}

	// TODO: Add support for Locations -- Get the locations (countries) that the list's subscribers have been tagged to based on geocoding their IP address.
//	public List<Locations> getActivity() {
//		final JSONObject list = new JSONObject(connection.do_Get(new URL(connection.getListendpoint()+"/"+getId()+"/locations"), connection.getApikey()));
//		final JSONArray rptArray = list.getJSONArray("locations");
//		ArrayList<Locations> reports = new ArrayList<Locations>(rptArray.length());
//		for (int i = 0 ; i < rptArray.length();i++)
//		{
//			final JSONObject rptDetail = rptArray.getJSONObject(i);
//			Locations report = new Locations(rptDetail);
//			reports.add(report);
//		}
//		return reports;
//	}

	// TODO: Add support for Preview Segment -- Provide conditions to preview segment.
	// TODO: Add support for Signup Forms - Manage list signup forms.
	// TODO: Add support for Webhooks -- Manage webhooks for a specific Mailchimp list.

	//
	// Members -- Manage members of a specific Mailchimp list, including currently subscribed, unsubscribed, and bounced members.
	//
	// TODO: Members > Events -- Use the Events endpoint to collect website or in-app actions and trigger targeted automations.
	// TODO: Members > Member Activity -- Get details about subscribers' recent activity.
	// TODO: Members > Member Goals -- Get information about recent goal events for a specific list member.
	
	/**
	 * Get information about members in this list with pagination
	 * @param count Number of members to return. Maximum value is 1000.
	 * @param offset Zero based offset
	 * @return List of members
	 * @throws Exception 
	 */
	public List<Member> getMembers(int count, int offset) throws Exception {
		if (count < 1 || count > 1000) {
			throw new InvalidParameterException("Page size must be 1-1000");
		}
		ArrayList<Member> members = new ArrayList<Member>();
		final JSONObject list = new JSONObject(getConnection().do_Get(new URL(getConnection().getListendpoint()+"/"+getId()+"/members?count="+count+"&offset="+offset),connection.getApikey()));

		final JSONArray membersArray = list.getJSONArray("members");

		for (int i = 0 ; i < membersArray.length();i++)
		{
			final JSONObject memberDetail = membersArray.getJSONObject(i);
	    	Member member = new Member(connection, memberDetail);
			members.add(member);
		}
		return members;
	}

	/**
	 * Get members iterator
	 * 
	 * Checked exceptions, including TransportException and JSONException, are
	 * warped in a RuntimeException to reduce the need for boilerplate code inside
	 * of lambdas.
	 * 
	 * @return Member iterator
	 */
	public Iterable<Member> getMembers() {
		final String baseURL = getConnection().getListendpoint()+"/"+getId()+"/members";
		return new ModelIterator<Member>(Member.class, baseURL, getConnection());
	}

	/**
	 * Get information about a specific list member, including a currently
	 * subscribed, unsubscribed, or bounced member.
	 * 
	 * @param subscriber The member's email address or subscriber hash
	 * @throws URISyntaxException 
	 * @throws TransportException 
	 * @throws MalformedURLException 
	 * @throws JSONException 
	 */
	public Member getMember(String subscriber) throws JSONException, MalformedURLException, TransportException, URISyntaxException {
		final JSONObject member = new JSONObject(getConnection().do_Get(new URL(getConnection().getListendpoint()+"/"+
				getId()+"/members/"+Member.subscriberHash(subscriber)),connection.getApikey()));
		return new Member(connection, member);
	}
	
	/**
	 * Add a member with the minimum of information
	 * 
	 * @param status       Subscriber’s current status
	 * @param emailAddress Email address for a subscriber
	 * @return The newly created member
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 */
	public Member addMember(MemberStatus status, String emailAddress)
			throws MalformedURLException, TransportException, URISyntaxException {
		JSONObject json = new JSONObject();
		json.put("email_address", emailAddress);
		json.put("status", status.toString());

		String results = getConnection().do_Post(new URL(connection.getListendpoint() + "/" + getId() + "/members"),
				json.toString(), connection.getApikey());
		Member member = new Member(connection, new JSONObject(results));
		return member;
	}

	/**
	 * Add a new member to the list. Member fields will be freshened from mailchimp.
	 * 
	 * @param member
	 * @return The member with fields freshened from mailchimp.
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 */
	public Member addMember(Member member) throws MalformedURLException, TransportException, URISyntaxException {
		JSONObject json = member.getJsonRepresentation();
		String results = connection.do_Post(new URL(connection.getListendpoint()+"/"+getId()+"/members"), json.toString(), connection.getApikey());
		member.parse(connection, new JSONObject(results));
        return member;
	}
	
	/**
	 * Add a member with first and last name.
	 * 
	 * @param status              Subscriber’s current status
	 * @param emailAddress        Email address for a subscriber
	 * @param merge_fields_values
	 * @return The newly added member
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 */
	public Member addMember(MemberStatus status, String emailAddress, HashMap<String, Object> merge_fields_values)
			throws TransportException, URISyntaxException, MalformedURLException {
		URL url = new URL(connection.getListendpoint() + "/" + getId() + "/members");

		JSONObject json = new JSONObject();
		JSONObject merge_fields = new JSONObject();

		Iterator<Entry<String, Object>> it = merge_fields_values.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Object> pair = it.next();
			it.remove(); // avoids a ConcurrentModificationException
			merge_fields.put(pair.getKey(), pair.getValue());
		}

		json.put("status", status.toString());
		json.put("email_address", emailAddress);
		json.put("merge_fields", merge_fields);
		String results = getConnection().do_Post(url, json.toString(), connection.getApikey());
		Member member = new Member(connection, new JSONObject(results));
		return member;
	}

	/**
	 * Update list subscriber via a PATCH operation. Member fields will be freshened
	 * from MailChimp.
	 * 
	 * @param member
	 * @return The member with fields freshened from mailchimp.
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 */
	public Member updateMember(Member member) throws MalformedURLException, TransportException, URISyntaxException {
		JSONObject json = member.getJsonRepresentation();

		String results = getConnection().do_Patch(
				new URL(connection.getListendpoint() + "/" + getId() + "/members/" + member.getId()), json.toString(),
				connection.getApikey());
		member.parse(connection, new JSONObject(results)); // update member object with current data
		return member;
	}

	/**
	 * Add or update a list member via a PUT operation. When a new member is added
	 * and no status_if_new has been specified SUBSCRIBED will be used. Member
	 * fields will be freshened from Milchimp.
	 * 
	 * Note that if an existing member (previously archived or otherwise) is updated
	 * member tags will not be applied. Use
	 * {@link com.github.bananaj.model.list.member.Member#applyTags(java.util.Map)}
	 * or
	 * {@link com.github.bananaj.model.list.member.Member#applyTag(String, com.github.bananaj.model.list.member.TagStatus)}
	 * 
	 * @param member
	 * @return The member with fields freshened from mailchimp.
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 */
	public Member addOrUpdateMember(Member member) throws MalformedURLException, TransportException, URISyntaxException {
		JSONObject json = member.getJsonRepresentation();

		if (member.getStatusIfNew() == null) {
			json.put("status_if_new", MemberStatus.SUBSCRIBED.toString());
		}

		String results = getConnection().do_Put(
				new URL(connection.getListendpoint() + "/" + getId() + "/members/" + member.getId()), json.toString(),
				connection.getApikey());
		member.parse(getConnection(), new JSONObject(results)); // update member object with current data
		return member;
	}

	/**
	 * Delete a member from list.
	 * 
	 * @param memberID
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 */
	public void deleteMember(String memberID)
			throws MalformedURLException, TransportException, URISyntaxException {
		getConnection().do_Delete(new URL(connection.getListendpoint() + "/" + getId() + "/members/" + memberID),
				connection.getApikey());
	}

	/**
	 * Permanently delete a member for list.
	 * 
	 * @param memberID
	 * @throws MalformedURLException
	 * @throws TransportException
	 * @throws URISyntaxException
	 */
	public void deleteMemberPermanent(String memberID)
			throws MalformedURLException, TransportException, URISyntaxException {
		getConnection().do_Post(new URL(getConnection().getListendpoint() + "/" + getId() + "/members/" + memberID
				+ "/actions/delete-permanent"), getConnection().getApikey());
	}

	//
	// Members > Member Tags -- Manage all the tags that have been assigned to a contact.
	//
	
	/**
	 * Get paginated tags for the specified audience member.
	 * 
	 * @param subscriber     The member's email address or subscriber hash
	 * @param count          Number of tags to return
	 * @param offset         Zero based offset
	 * @throws Exception
	 */
	public List<MemberTag> getMemberTags(String subscriber, int count, int offset) throws Exception {
		if (count < 1 || count > 1000) {
			throw new InvalidParameterException("Page size must be 1-1000");
		}
		final JSONObject tagsObj = new JSONObject(getConnection().do_Get(new URL(getConnection().getListendpoint() + "/"
				+ getId() + "/members/" + Member.subscriberHash(subscriber) + 
				"/tags" + "?offset=" + offset + "&count=" + count),
				getConnection().getApikey()));
		// int total_items = tagsObj.getInt("total_items");	// The total number of items matching the query regardless of pagination
		// matching the query regardless of pagination
		final JSONArray tagsArray = tagsObj.getJSONArray("tags");
		List<MemberTag> tags = new ArrayList<MemberTag>(tagsArray.length());
		for (int i = 0; i < tagsArray.length(); i++) {
			tags.add(new MemberTag(tagsArray.getJSONObject(i)));
		}
		return tags;
	}

	/**
	 * Get tags iterator for the specified audience member.
	 * 
	 * Checked exceptions, including TransportException and JSONException, are
	 * warped in a RuntimeException to reduce the need for boilerplate code inside
	 * of lambdas.
	 * 
	 * @return MemberTag iterator
	 */
	public Iterable<MemberTag> getMemberTags(String subscriber) {
		final String baseURL = getConnection().getListendpoint()+"/"+getId()+"/members/" + 
				Member.subscriberHash(subscriber) + "/tags";
		return new ModelIterator<MemberTag>(MemberTag.class, baseURL, getConnection());
	}

	//
	// Members > Member Notes -- Manage recent notes for a specific list member.
	//
	
	/**
	 * Get recent notes for this list member.
	 * 
	 * @param subscriber     The member's email address or subscriber hash
	 * @param count          Number of items to return
	 * @param offset         Zero based offset
	 * @throws JSONException
	 * @throws Exception
	 */
	public List<MemberNote> getMemberNotes(String subscriber, int count, int offset) throws Exception {
		if (count < 1 || count > 1000) {
			throw new InvalidParameterException("Page size must be 1-1000");
		}
		final JSONObject noteObj = new JSONObject(getConnection().do_Get(new URL(getConnection().getListendpoint()+"/"+
				getId()+"/members/"+Member.subscriberHash(subscriber)+
				"/notes?count="+count+"&offset="+offset), getConnection().getApikey()));
		//String email_id = noteObj.getString("email_id");
		//String list_id = noteObj.getString("list_id");
		//int total_items = noteObj.getInt("total_items");	// The total number of items matching the query regardless of pagination
		final JSONArray noteArray = noteObj.getJSONArray("notes");
		List<MemberNote> notes = new ArrayList<MemberNote>(noteArray.length());

		for (int i = 0 ; i < noteArray.length();i++)
		{
			notes.add(new MemberNote(noteArray.getJSONObject(i)));
		}

		return notes;
	}
	
	/**
	 * Get iterator for recent notes for the specified member.
	 * 
	 * Checked exceptions, including TransportException and JSONException, are
	 * warped in a RuntimeException to reduce the need for boilerplate code inside
	 * of lambdas.
	 * 
	 * @param subscriber     The member's email address or subscriber hash
	 * @return MemberNote iterator
	 */
	public Iterable<MemberNote> getMemberNotes(String subscriber) {
		final String baseURL = getConnection().getListendpoint()+"/"+getId()+"/members/"+
				Member.subscriberHash(subscriber)+"/notes";
		return new ModelIterator<MemberNote>(MemberNote.class, baseURL, getConnection());
	}

	/**
	 * Get a specific note for the member
	 * 
	 * @param subscriber The member's email address or subscriber hash
	 * @param noteId         The id for the note.
	 * @throws JSONException
	 * @throws MalformedURLException
	 * @throws TransportException
	 * @throws URISyntaxException
	 */
	public MemberNote getMemberNote(String subscriber, int noteId)
			throws JSONException, MalformedURLException, TransportException, URISyntaxException {
		final JSONObject noteObj = new JSONObject(getConnection().do_Get(new URL(
				getConnection().getListendpoint() + "/" + getId() + "/members/" + Member.subscriberHash(subscriber) + 
				"/notes/" + noteId),
				getConnection().getApikey()));
		return new MemberNote(noteObj);

	}

	//
	// Growth History -- View a summary of the month-by-month growth activity 
	//                   for the list/audience.
	//
	//
	
	/**
	 * Get a summary of the month-by-month growth activity for this list/audience.
	 * @param count Number of reports to return. Maximum value is 1000.
	 * @param offset Zero based offset
	 * @param dir Optional, determines the order direction for sorted results.
	 * @return Summary of the month-by-month growth activity for this list/audience.
	 * @throws JSONException
	 * @throws MalformedURLException
	 * @throws TransportException
	 * @throws URISyntaxException
	 */
	public List<GrowthHistory> getGrowthHistory(int count, int offset, SortDirection dir) throws JSONException, MalformedURLException, TransportException, URISyntaxException {
		String response = connection.do_Get(new URL(connection.getListendpoint() + "/" + getId() + 
				"/growth-history?count="+count+"&offset="+offset+
				"&sort_field=month&sort_dir="+(dir != null ? dir.toString() : SortDirection.DESC.toString())), connection.getApikey());
		JSONObject jsonObj = new JSONObject(response);
		
		//int totalItems = jsonObj.getInt("total_items");	// The total number of items matching the query regardless of pagination
        //String listId = jsonObj.getString("list_id");
        JSONArray history = jsonObj.getJSONArray("history");
        
        ArrayList<GrowthHistory> growthHistory = new ArrayList<GrowthHistory>(history.length());
        for (int i = 0; i<history.length(); i++) {
        	JSONObject gh = (JSONObject)history.get(i);
        	growthHistory.add(new GrowthHistory(gh));
        }
        
        return growthHistory;
	}

	//
	// Interest Categories -- Manage interest categories for a specific list. Interest categories 
	//                        organize interests, which are used to group subscribers based on their 
	//                        preferences. These correspond to 'group titles' in the Mailchimp application.
	//
	
	/**
	 * Get interest categories for list. These correspond to ‘group titles’ in the
	 * MailChimp application.
	 * 
	 * @param count  Number of items to return
	 * @param offset Zero based offset
	 * @return List of interest categories
	 * @throws Exception
	 */
	public List<InterestCategory> getInterestCategories(int count, int offset)
			throws Exception {
		if (count < 1 || count > 1000) {
			throw new InvalidParameterException("Page size must be 1-1000");
		}
		ArrayList<InterestCategory> categories = new ArrayList<InterestCategory>();
		JSONObject list = new JSONObject(getConnection().do_Get(new URL(connection.getListendpoint() + "/" + getId()
				+ "/interest-categories?count=" + count + "&offset=" + offset), connection.getApikey()));
		JSONArray categoryArray = list.getJSONArray("categories");

		for (int i = 0; i < categoryArray.length(); i++) {
			final JSONObject jsonCategory = categoryArray.getJSONObject(i);
			InterestCategory category = new InterestCategory(connection, jsonCategory);
			categories.add(category);

		}
		return categories;
	}

	/**
	 * Get interest categories iterator for list. These correspond to ‘group titles’ in the
	 * MailChimp application.
	 * 
	 * Checked exceptions, including TransportException and JSONException, are
	 * warped in a RuntimeException to reduce the need for boilerplate code inside
	 * of lambdas.
	 * 
	 * @return InterestCategory iterator
	 */
	public Iterable<InterestCategory> getInterestCategories() {
		final String baseURL = getConnection().getListendpoint()+"/"+getId()+"/interest-categories";
		return new ModelIterator<InterestCategory>(InterestCategory.class, baseURL, getConnection());
	}

	/**
	 * Get the interest category details given its Id.
	 * 
	 * @param interestCategoryId
	 * @return
	 * @throws JSONException
	 * @throws MalformedURLException
	 * @throws TransportException
	 * @throws URISyntaxException
	 */
	public InterestCategory getInterestCategory(String interestCategoryId)
			throws JSONException, MalformedURLException, TransportException, URISyntaxException {
		JSONObject jsonCategory = new JSONObject(connection.do_Get(
				new URL(connection.getListendpoint() + "/" + getId() + "/interest-categories/" + interestCategoryId),
				connection.getApikey()));
		return new InterestCategory(connection, jsonCategory);
	}

	/**
	 * Remove an interest category from list.
	 * 
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 */
	public InterestCategory addInrestCategory(InterestCategory category)
			throws MalformedURLException, TransportException, URISyntaxException {
		JSONObject json = category.getJsonRepresentation();
		String results = getConnection().do_Post(
				new URL(getConnection().getListendpoint() + "/" + getId() + "/interest-categories"), json.toString(),
				getConnection().getApikey());
		return new InterestCategory(connection, new JSONObject(results));
	}

	/**
	 * Remove an interest category from list.
	 * 
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 */
	public void deleteInrestCategory(String categoryId)
			throws MalformedURLException, TransportException, URISyntaxException {
		getConnection().do_Delete(
				new URL(getConnection().getListendpoint() + "/" + getId() + "/interest-categories/" + categoryId),
				getConnection().getApikey());
	}

	//
	// Interest Categories > Interests 
	//     Manage interests for a specific Mailchimp list. Assign subscribers to interests 
	//     to group them together. Interests are referred to as 'group names' in the 
	//     Mailchimp application.
	//
	
	/**
	 * Get interests for this list. Interests are referred to as ‘group names’ in
	 * the MailChimp application.
	 * 
	 * @param interestCategoryId
	 * @param count              Number of members to return. Maximum value is 1000.
	 * @param offset             Zero based offset
	 * @return List of interests for this list
	 * @throws Exception
	 */
	public List<Interest> getInterests(String interestCategoryId, int count, int offset)
			throws Exception {
		if (count < 1 || count > 1000) {
			throw new InvalidParameterException("Page size must be 1-1000");
		}
		ArrayList<Interest> interests = new ArrayList<Interest>();
		JSONObject list = new JSONObject(
				connection.do_Get(
						new URL(connection.getListendpoint() + "/" + getId() + "/interest-categories/"
								+ interestCategoryId + "/interests?count=" + count + "&offset=" + offset),
						connection.getApikey()));
		JSONArray interestArray = list.getJSONArray("interests");

		for (int i = 0; i < interestArray.length(); i++) {
			final JSONObject jsonInterest = interestArray.getJSONObject(i);
			Interest interest = new Interest(connection, jsonInterest);
			interests.add(interest);

		}
		return interests;
	}

	/**
	 * Get interests iterator for this list. Interests are referred to as ‘group
	 * names’ in the MailChimp application.
	 * 
	 * Checked exceptions, including TransportException and JSONException, are
	 * warped in a RuntimeException to reduce the need for boilerplate code inside
	 * of lambdas.
	 * 
	 * @param interestCategoryId
	 * @return Interest iterator
	 */
	public Iterable<Interest> getInterests(String interestCategoryId) {
		final String baseURL = getConnection().getListendpoint() + "/" + getId() + "/interest-categories/"
				+ interestCategoryId + "/interests";
		return new ModelIterator<Interest>(Interest.class, baseURL, getConnection());
	}

	/**
	 * Get a specific interests for this list. Interests are referred to as ‘group
	 * names’ in the MailChimp application.
	 * 
	 * @param interestCategoryId
	 * @param interestId
	 * @return The requested interest
	 * @throws JSONException
	 * @throws MalformedURLException
	 * @throws TransportException
	 * @throws URISyntaxException
	 */
	public Interest getInterest(String interestCategoryId, String interestId)
			throws JSONException, MalformedURLException, TransportException, URISyntaxException {
		JSONObject jsonInterests = new JSONObject(connection.do_Get(new URL(connection.getListendpoint() + "/" + getId()
				+ "/interest-categories/" + interestCategoryId + "/interests/" + interestId), connection.getApikey()));
		return new Interest(connection, jsonInterests);
	}

	//
	// Segments -- Manage segments and tags for a specific Mailchimp list. 
	//             A segment is a section of your list that includes only those 
	//             subscribers who share specific common field information. Tags 
	//             are labels you create to help organize your contacts.
	//
	
	/**
	 * Get all segments of this list. A segment is a section of your list that
	 * includes only those subscribers who share specific common field information.
	 * 
	 * @param count  Number of templates to return
	 * @param offset Zero based offset
	 * @return List containing segments
	 * @throws Exception
	 */
	public List<Segment> getSegments(int count, int offset)
			throws Exception {
		if (count < 1 || count > 1000) {
			throw new InvalidParameterException("Page size must be 1-1000");
		}
		ArrayList<Segment> segments = new ArrayList<Segment>();
		JSONObject jsonSegments = new JSONObject(connection.do_Get(new URL(
				connection.getListendpoint() + "/" + getId() + "/segments?offset=" + offset + "&count=" + count),
				connection.getApikey()));

		final JSONArray segmentsArray = jsonSegments.getJSONArray("segments");

		for (int i = 0; i < segmentsArray.length(); i++) {
			final JSONObject segmentDetail = segmentsArray.getJSONObject(i);
			Segment segment = new Segment(getConnection(), segmentDetail);
			segments.add(segment);
		}

		return segments;
	}

	/**
	 * Get segments iterator for this list. A segment is a section of your list that
	 * includes only those subscribers who share specific common field information.
	 * 
	 * Checked exceptions, including TransportException and JSONException, are
	 * warped in a RuntimeException to reduce the need for boilerplate code inside
	 * of lambdas.
	 * 
	 * @return Segment iterator
	 */
	public Iterable<Segment> getSegments() {
		final String baseURL = getConnection().getListendpoint()+"/"+getId()+"/segments";
		return new ModelIterator<Segment>(Segment.class, baseURL, getConnection());
	}

	/**
	 * Get all segments of this list. A segment is a section of your list that
	 * includes only those subscribers who share specific common field information.
	 * 
	 * @param type   Limit results based on segment type
	 * @param count  Number of templates to return
	 * @param offset Zero based offset
	 * @return List containing segments of the specified type
	 * @throws JSONException
	 * @throws MalformedURLException
	 * @throws TransportException
	 * @throws URISyntaxException
	 */
	public List<Segment> getSegments(SegmentType type, int count, int offset)
			throws JSONException, MalformedURLException, TransportException, URISyntaxException {
		ArrayList<Segment> segments = new ArrayList<Segment>();
		JSONObject jsonSegments = new JSONObject(
				connection.do_Get(new URL(connection.getListendpoint() + "/" + getId() + "/segments?offset=" + offset
						+ "&count=" + count + "&type" + type.toString()), connection.getApikey()));

		final JSONArray segmentsArray = jsonSegments.getJSONArray("segments");

		for (int i = 0; i < segmentsArray.length(); i++) {
			final JSONObject segmentDetail = segmentsArray.getJSONObject(i);
			Segment segment = new Segment(getConnection(), segmentDetail);
			segments.add(segment);
		}

		return segments;
	}

	/**
	 * Get a specific segment of this list
	 * 
	 * @param segmentID
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 * @throws JSONException
	 */
	public Segment getSegment(String segmentID)
			throws JSONException, MalformedURLException, TransportException, URISyntaxException {
		String response = connection.do_Get(
				new URL(connection.getListendpoint() + "/" + getId() + "/segments/" + segmentID),
				connection.getApikey());
		JSONObject jsonSegment = new JSONObject(response);
		return new Segment(getConnection(), jsonSegment);
	}

	/**
	 * Add a segment to the list
	 * 
	 * @param name   The name of the segment.
	 * @param option The conditions of the segment. Static and fuzzy segments don't
	 *               have conditions.
	 * @return The newly created segment
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 */
	public Segment addSegment(String name, SegmentOptions option)
			throws MalformedURLException, TransportException, URISyntaxException {
		JSONObject segment = new JSONObject();
		segment.put("name", name);
		if (option != null) {
			segment.put("options", option.getJsonRepresentation());
		}

		String response = connection.do_Post(new URL(connection.getListendpoint() + "/" + getId() + "/segments"),
				segment.toString(), connection.getApikey());
		JSONObject jsonSegment = new JSONObject(response);
		return new Segment(getConnection(), jsonSegment);
	}

	/**
	 * Add a static segment with a name and predefined emails to this list. Every
	 * E-Mail address which is not present on the list itself will be ignored and
	 * not added to the static segment.
	 * 
	 * @param name   The name of the segment.
	 * @param emails An array of emails to be used for a static segment. Any emails
	 *               provided that are not present on the list will be ignored.
	 *               Passing an empty array will create a static segment without any
	 *               subscribers.
	 * @return The newly created segment
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 * @throws EmailException
	 */
	public Segment addStaticSegment(String name, String... emails)
			throws MalformedURLException, TransportException, URISyntaxException, EmailException {
		JSONObject segment = new JSONObject();
		segment.put("name", name);
		for (String email : emails) {
			if (!EmailValidator.getInstance().validate(email)) {
				throw new EmailException(email);
			}
		}
		segment.put("static_segment", emails);
		String response = getConnection().do_Post(new URL(connection.getListendpoint() + "/" + getId() + "/segments"),
				segment.toString(), connection.getApikey());
		JSONObject jsonSegment = new JSONObject(response);
		return new Segment(getConnection(), jsonSegment);
	}

	/**
	 * Delete a specific segment.
	 * 
	 * @param segmentId
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws MalformedURLException
	 */
	public void deleteSegment(String segmentId) throws MalformedURLException, TransportException, URISyntaxException {
		connection.do_Delete(new URL(connection.getListendpoint() + "/" + getId() + "/segments/" + segmentId),
				connection.getApikey());
	}

	//
	// Merge Fields -- Manage merge fields (formerly merge vars) for the list.
	//
	
	/**
	 * Get a merge fields for this list.
	 * 
	 * @param count The number of records to return. Maximum value is 1000.
	 * @param offset Zero based offset
	 * @throws Exception
	 */
	public List<MergeField> getMergeFields(int count, int offset) throws Exception {
		if (count < 1 || count > 1000) {
			throw new InvalidParameterException("Page size must be 1-1000");
		}
		ArrayList<MergeField> mergeFields = new ArrayList<MergeField>();
		URL url = new URL(connection.getListendpoint()+"/"+getId()+"/merge-fields?offset=" + offset + "&count=" + count); // Note: Mailchimp currently supports a maximim of 80 merge fields

		JSONObject merge_fields = new JSONObject(connection.do_Get(url, connection.getApikey()));
		final JSONArray mergeFieldsArray = merge_fields.getJSONArray("merge_fields");

		for (int i = 0 ; i < mergeFieldsArray.length(); i++) {
			final JSONObject mergeFieldDetail = mergeFieldsArray.getJSONObject(i);
			MergeField mergeField = new MergeField(getConnection(), mergeFieldDetail);
			mergeFields.add(mergeField);
		}
		return mergeFields;
	}

	/**
	 * Get iterator of merge fields for this list.
	 * 
	 * Checked exceptions, including TransportException and JSONException, are
	 * warped in a RuntimeException to reduce the need for boilerplate code inside
	 * of lambdas.
	 * 
	 * @return MergeField iterator
	 */
	public Iterable<MergeField> getMergeFields() {
		final String baseURL = getConnection().getListendpoint()+"/"+getId()+"/merge-fields";
		return new ModelIterator<MergeField>(MergeField.class, baseURL, getConnection());
	}

	/**
	 * Get a specific merge field of this list/audience.
	 * 
	 * @param mergeFieldID
	 * @throws URISyntaxException
	 * @throws TransportException
	 * @throws JSONException
	 * @throws MalformedURLException
	 */
	public MergeField getMergeField(String mergeFieldID) throws JSONException, TransportException, URISyntaxException, MalformedURLException {
		URL url = new URL(connection.getListendpoint()+"/"+getId()+"/merge-fields/"+mergeFieldID);
		JSONObject mergeFieldJSON = new JSONObject(connection.do_Get(url,connection.getApikey()));
		return new MergeField(connection, mergeFieldJSON);
	}

	/**
	 * Add a merge field to this list/audience
	 * @param mergeFieldtoAdd
	 * @return The new Mailchimp merge field that was added.
	 * @throws JSONException
	 * @throws TransportException
	 * @throws URISyntaxException
	 * @throws MalformedURLException
	 */
	public MergeField addMergeField(MergeField mergeFieldtoAdd) throws JSONException, TransportException, URISyntaxException, MalformedURLException {
		URL url = new URL(connection.getListendpoint()+"/"+getId()+"/merge-fields");
		JSONObject mergeFieldJSON = new JSONObject(connection.do_Post(url, mergeFieldtoAdd.getJsonRepresentation().toString(), connection.getApikey()));
		return new MergeField(connection, mergeFieldJSON);
	}


	public void deleteMergeField(String mergeFieldID) throws MalformedURLException, TransportException, URISyntaxException {
		connection.do_Delete(new URL(connection.getListendpoint()+"/"+getId()+"/merge-fields/"+mergeFieldID), connection.getApikey());
	}

	/**
	 * A string that uniquely identifies this list.
	 */
	public String getId() {
		return id;
	}

	/**
	 * The ID used in the Mailchimp web application. View this list in your Mailchimp 
	 * account at https://{dc}.admin.mailchimp.com/lists/members/?id={web_id}.
	 */
	public int getWebId() {
		return webId;
	}

	/**
	 * The name of the list/audience.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name of the list/audience. You must call {@link #update()}
	 *             for changes to take effect.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Contact information displayed in campaign footers to comply with
	 * international spam laws.
	 */
	public ListContact getContact() {
		return contact;
	}

	/**
	 * @param contact Contact information displayed in campaign footers to comply
	 *                with international spam laws. You must call {@link #update()}
	 *                for changes to take effect.
	 */
	public void setContact(ListContact contact) {
		this.contact = contact;
	}

	/**
	 * The permission reminder for the list.
	 */
	public String getPermissionReminder() {
		return permissionReminder;
	}

	/**
	 * @param permissionReminder The permission reminder for the list. You must call
	 *                           {@link #update()} for changes to take effect.
	 */
	public void setPermissionReminder(String permissionReminder) {
		this.permissionReminder = permissionReminder;
	}

	/**
	 * Whether campaigns for this list use the Archive Bar in archives by default.
	 */
	public boolean isUseArchiveBar() {
		return useArchiveBar;
	}

	/**
	 * @param useArchiveBar Whether campaigns for this list use the Archive Bar in
	 *                      archives by default. You must call {@link #update()} for
	 *                      changes to take effect.
	 */
	public void setUseArchiveBar(boolean useArchiveBar) {
		this.useArchiveBar = useArchiveBar;
	}

	/**
	 * Default values for campaigns created for this list.
	 */
	public ListCampaignDefaults getCampaignDefaults() {
		return campaignDefaults;
	}

	/**
	 * @param campaignDefaults Default values for campaigns created for this list.
	 *                         You must call {@link #update()} for changes to take
	 *                         effect.
	 */
	public void setCampaignDefaults(ListCampaignDefaults campaignDefaults) {
		this.campaignDefaults = campaignDefaults;
	}

	/**
	 * The email address to send subscribe notifications to.
	 */
	public String getNotifyOnSubscribe() {
		return notifyOnSubscribe;
	}

	/**
	 * @param notifyOnSubscribe The email address to send subscribe notifications
	 *                          to. You must call {@link #update()} for changes to
	 *                          take effect.
	 */
	public void setNotifyOnSubscribe(String notifyOnSubscribe) {
		this.notifyOnSubscribe = notifyOnSubscribe;
	}

	/**
	 * The email address to send unsubscribe notifications to.
	 */
	public String getNotifyOnUnsubscribe() {
		return notifyOnUnsubscribe;
	}

	/**
	 * @param notifyOnUnsubscribe The email address to send unsubscribe
	 *                            notifications to. You must call {@link #update()}
	 *                            for changes to take effect.
	 */
	public void setNotifyOnUnsubscribe(String notifyOnUnsubscribe) {
		this.notifyOnUnsubscribe = notifyOnUnsubscribe;
	}

	/**
	 * The date and time that this list was created.
	 * 
	 * @return the dateCreated
	 */
	public ZonedDateTime getDateCreated() {
		return dateCreated;
	}

	/**
	 * An auto-generated activity score for the list (0-5).
	 */
	public int getListRating() {
		return listRating;
	}

	/**
	 * Whether the list supports multiple formats for emails. When set to true,
	 * subscribers can choose whether they want to receive HTML or plain-text
	 * emails. When set to false, subscribers will receive HTML emails, with a
	 * plain-text alternative backup.
	 */
	public boolean isEmailTypeOption() {
		return emailTypeOption;
	}

	/**
	 * @param emailTypeOption Whether the list supports multiple formats for emails.
	 *                        When set to true, subscribers can choose whether they
	 *                        want to receive HTML or plain-text emails. When set to
	 *                        false, subscribers will receive HTML emails, with a
	 *                        plain-text alternative backup. You must call
	 *                        {@link #update()} for changes to take effect.
	 */
	public void setEmailTypeOption(boolean emailTypeOption) {
		this.emailTypeOption = emailTypeOption;
	}

	/**
	 * Mailchimp EepURL shortened version of this list's subscribe form.
	 */
	public String getSubscribeUrlShort() {
		return subscribeUrlShort;
	}

	/**
	 * The full version of this list’s subscribe form (host will vary).
	 */
	public String getSubscribeUrlLong() {
		return subscribeUrlLong;
	}

	/**
	 * The list’s Email Beamer address.
	 */
	public String getBeamerAddress() {
		return beamerAddress;
	}

	/**
	 * Whether this list is public or private.
	 */
	public ListVisibility getVisibility() {
		return visibility;
	}

	/**
	 * @param visibility Whether this list is public or private. You must call
	 *                   {@link #update()} for changes to take effect.
	 */
	public void setVisibility(ListVisibility visibility) {
		this.visibility = visibility;
	}

	/**
	 * Whether or not to require the subscriber to confirm subscription via email.
	 */
	public boolean isDoubleOptin() {
		return doubleOptin;
	}

	/**
	 * @param doubleOptin Whether or not to require the subscriber to confirm
	 *                    subscription via email. You must call {@link #update()}
	 *                    for changes to take effect.
	 */
	public void setDoubleOptin(boolean doubleOptin) {
		this.doubleOptin = doubleOptin;
	}

	/**
	 * Whether or not this list has a welcome automation connected. Welcome
	 * Automations: welcomeSeries, singleWelcome, emailFollowup.
	 */
	public boolean isHasWelcome() {
		return hasWelcome;
	}

	/**
	 * Whether or not the list has marketing permissions (eg. GDPR) enabled.
	 */
	public boolean isMarketingPermissions() {
		return marketingPermissions;
	}

	/**
	 * Stats for the list. Many of these are cached for at least five minutes.
	 * 
	 * @return the list stats
	 */
	public ListStats getStats() {
		return stats;
	}

	/**
	 *
	 * @return the MailChimp com.github.bananaj.connection.
	 */
	public MailChimpConnection getConnection() {
		return connection;
	}

	/**
	 * @return the jsonRepresentation
	 */
	public JSONObject getJSONRepresentation() {
		JSONObject jsonList = new JSONObject();
		
		jsonList.put("name",name);
		jsonList.put("contact", contact.getJSONRepresentation());
		jsonList.put("permission_reminder", permissionReminder);
		jsonList.put("use_archive_bar", useArchiveBar);
		jsonList.put("campaign_defaults", campaignDefaults.getJSONRepresentation());
		jsonList.put("notify_on_subscribe", notifyOnSubscribe);
		jsonList.put("notify_on_unsubscribe", notifyOnUnsubscribe);
		jsonList.put("email_type_option", emailTypeOption);
		jsonList.put("visibility", visibility.toString());
		jsonList.put("double_optin", doubleOptin);
		jsonList.put("marketing_permissions", marketingPermissions);

		return jsonList;
	}
	
	/**
	 * Update list/audience via a PATCH operation. Member fields will be freshened.
	 * @throws MalformedURLException
	 * @throws TransportException
	 * @throws URISyntaxException
	 */
	public void update() throws MalformedURLException, TransportException, URISyntaxException {
		String results = connection.do_Patch(new URL(connection.getListendpoint()+"/"+getId()), getJSONRepresentation().toString(), connection.getApikey());
		parse(connection, new JSONObject(results));  // update this object with current data
	}
	
	/**
	 * Delete this list/audience from your account
	 * @throws URISyntaxException 
	 * @throws TransportException 
	 * @throws MalformedURLException 
	 */
	public void delete() throws MalformedURLException, TransportException, URISyntaxException {
		connection.do_Delete(new URL(connection.getListendpoint() +"/"+getId()), connection.getApikey());
	}
	
	@Override
	public String toString() {
		return 
				"Audience:" + System.lineSeparator() +
				"    Id: " + getId() + System.lineSeparator() +
				"    Web Id: " + getWebId() + System.lineSeparator() +
				"    Name: " + getName() + System.lineSeparator() +
				"    Permission Reminder: " + isMarketingPermissions() + System.lineSeparator() +
				"    Use Archive Bar: " + isUseArchiveBar() + System.lineSeparator() +
				"    Notify On Subscribe: " + getNotifyOnSubscribe() + System.lineSeparator() +
				"    Notify On Unsubscribe: " + getNotifyOnUnsubscribe() + System.lineSeparator() +
				"    Created: " + DateConverter.toLocalString(getDateCreated()) + System.lineSeparator() +
				"    List Rating: " + getListRating() + System.lineSeparator() +
				"    Email Type Option: " + isEmailTypeOption() + System.lineSeparator() +
				"    Subscribe URL Short: " + getSubscribeUrlShort() + System.lineSeparator() +
				"    Subscribe URL Long: " + getSubscribeUrlLong() + System.lineSeparator() +
				"    Beamer Address: " + getBeamerAddress() + System.lineSeparator() +
				"    Visibility: " + getVisibility().toString() + System.lineSeparator() +
				"    Double Option: " + isDoubleOptin() + System.lineSeparator() +
				"    Has Welcome: " + isHasWelcome() + System.lineSeparator() +
				"    Marketing Permissions: " + isMarketingPermissions() + System.lineSeparator() +
				getContact().toString() + System.lineSeparator() +
				getCampaignDefaults().toString() + System.lineSeparator() +
				getStats().toString();
	}

    /**
     * Builder for {@link MailChimpList}
     *
     */
    public static class Builder {
        private MailChimpConnection connection;
    	private String name;			// The name of the list
    	private ListContact contact;	// Contact information displayed in campaign footers to comply with international spam laws
    	private String permissionReminder;	// The permission reminder for the list
    	private boolean useArchiveBar = false;	// Whether campaigns for this list use the Archive Bar in archives by default
    	private ListCampaignDefaults campaignDefaults;	// Default values for campaigns created for this list
    	private String notifyOnSubscribe;	// The email address to send subscribe notifications to
    	private String notifyOnUnsubscribe; // The email address to send unsubscribe notifications to 
    	private boolean emailTypeOption = false;	// Whether the list supports multiple formats for emails. When set to true, subscribers can choose whether they want to receive HTML or plain-text emails. When set to false, subscribers will receive HTML emails, with a plain-text alternative backup.
    	private ListVisibility visibility = ListVisibility.PRIVATE;	// Whether this list is public or private (pub, prv)
    	private boolean doubleOptin = false;	// Whether or not to require the subscriber to confirm subscription via email
    	private boolean marketingPermissions = false;	// Whether or not the list has marketing permissions (eg. GDPR) enabled

		/**
		 * @param connection the connection to set
		 */
		public Builder connection(MailChimpConnection connection) {
			this.connection = connection;
			return this;
		}

		/**
		 * @param name The name of the list/audience.
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		/**
		 * @param contact Contact information displayed in campaign footers to comply
		 *                with international spam laws.
		 */
		public Builder contact(ListContact contact) {
			this.contact = contact;
			return this;
		}

		/**
		 * @param permissionReminder The permission reminder for the list.
		 */
		public Builder permissionReminder(String permissionReminder) {
			this.permissionReminder = permissionReminder;
			return this;
		}

		/**
		 * @param useArchiveBar Whether campaigns for this list use the Archive Bar in
		 *                      archives by default.
		 */
		public Builder useArchiveBar(boolean useArchiveBar) {
			this.useArchiveBar = useArchiveBar;
			return this;
		}

		/**
		 * @param campaignDefaults Default values for campaigns created for this list.
		 */
		public Builder campaignDefaults(ListCampaignDefaults campaignDefaults) {
			this.campaignDefaults = campaignDefaults;
			return this;
		}

		/**
		 * @param notifyOnSubscribe The email address to send subscribe notifications
		 *                          to.
		 */
		public Builder notifyOnSubscribe(String notifyOnSubscribe) {
			this.notifyOnSubscribe = notifyOnSubscribe;
			return this;
		}

		/**
		 * @param notifyOnUnsubscribe The email address to send unsubscribe
		 *                            notifications to.
		 */
		public Builder notifyOnUnsubscribe(String notifyOnUnsubscribe) {
			this.notifyOnUnsubscribe = notifyOnUnsubscribe;
			return this;
		}

		/**
		 * @param emailTypeOption Whether the list supports multiple formats for emails.
		 *                        When set to true, subscribers can choose whether they
		 *                        want to receive HTML or plain-text emails. When set to
		 *                        false, subscribers will receive HTML emails, with a
		 *                        plain-text alternative backup.
		 */
		public Builder emailTypeOption(boolean emailTypeOption) {
			this.emailTypeOption = emailTypeOption;
			return this;
		}

		/**
		 * @param visibility Whether this list is public or private.
		 */
		public Builder visibility(ListVisibility visibility) {
			this.visibility = visibility;
			return this;
		}

		/**
		 * @param doubleOptin Whether or not to require the subscriber to confirm
		 *                    subscription via email.
		 */
		public Builder doubleOptin(boolean doubleOptin) {
			this.doubleOptin = doubleOptin;
			return this;
		}

		/**
		 * @param marketingPermissions Whether or not the list has marketing permissions
		 *                             (eg. GDPR) enabled.
		 */
		public Builder marketingPermissions(boolean marketingPermissions) {
			this.marketingPermissions = marketingPermissions;
			return this;
		}

    	public MailChimpList build() {
			Objects.requireNonNull(name, "name");
			Objects.requireNonNull(contact, "contact");
			Objects.requireNonNull(permissionReminder, "permission_reminder");
			Objects.requireNonNull(campaignDefaults, "campaign_defaults");
			//Objects.requireNonNull(emailTypeOption, "email_type_option");
    		return new MailChimpList(this);
    	}
    }
}

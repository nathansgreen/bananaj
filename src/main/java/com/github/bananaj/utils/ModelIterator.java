package com.github.bananaj.utils;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.bananaj.connection.MailChimpConnection;
import com.github.bananaj.exceptions.TransportException;
import com.github.bananaj.model.JSONParser;

public class ModelIterator<T extends JSONParser> implements Iterable<T> {
	
	MailChimpConnection connection;
	Queue<T> q = new LinkedList<>();
	private String query;
	private int offset = 0;
	private int pagesize = 1000;
	private Class<T> typeClasse;
	private Integer totalItems;
	private int currentIndex = 0;
	
	public ModelIterator(Class<T> typeClasse, String query, MailChimpConnection connection) {
		this.typeClasse = typeClasse;
		this.connection = connection;
		this.query = query;
		readPagedEntities();
	}

	public ModelIterator(Class<T> typeClasse, String query, MailChimpConnection connection, int pagesize) {
		this.typeClasse = typeClasse;
		this.connection = connection;
		this.query = query;
		this.pagesize = Math.min(1000, Math.max(pagesize, 1));
		readPagedEntities();
	}

	private void readPagedEntities() {
		try {
			URL url = new URL(query + (query.contains("?") ? "&" : "?") + "count="+pagesize + "&offset="+offset);
			offset += pagesize;
			final JSONObject list = new JSONObject(connection.do_Get(url,connection.getApikey()));

			if (list.has("total_items")) {
				totalItems = list.getInt("total_items");	// The total number of items matching the query regardless of pagination
			}

			Iterator<String> keys = list.keys();
			while(keys.hasNext()) {
				final String key = keys.next();
				if (key.equals("_links")) { continue; }
				final Object keyValue = list.get(key);
				if (keyValue instanceof JSONArray) { // look for main entity array
					// TODO: TRACE -- found 'key' entity array of type T
					final JSONArray entArray = (JSONArray)keyValue;
					for (int i = 0 ; i < entArray.length();i++)
					{
						final JSONObject objDetail = entArray.getJSONObject(i);
						T ent = typeClasse.getDeclaredConstructor().newInstance();
						ent.parse(connection, objDetail);
						q.offer(ent);
					}
					break;	// found entity array, no need to keep looking
				}
			}
		} catch (InstantiationException|NoSuchMethodException| InvocationTargetException e) {
			throw new RuntimeException("Class " + typeClasse.getCanonicalName() + " missing default constructor", e);  
		} catch (TransportException | JSONException |  IllegalAccessException | 
				MalformedURLException | URISyntaxException e) {
			// Wrap checked exceptions in a RuntimeException.
			// Checked exceptions are warped in a RuntimeException to reduce the need for
			// boilerplate code inside of lambdas.
			throw new RuntimeException(e);  
		} 
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {

			@Override
			public boolean hasNext() {
				if (q.peek() != null || (totalItems != null && currentIndex < totalItems)) {
					return true;
				}
				return false;
			}

			@Override
			public T next() {
				currentIndex++;
				T element = q.poll();
				if (element == null || q.peek() == null) {
					if (totalItems == null || currentIndex < totalItems) {
						// query for next page of entities
						try {
							readPagedEntities();
							element = q.poll();
						} catch (Exception ex) {
							throw new NoSuchElementException(ex.getMessage());
						}
					} 

					if (element == null ) {
						throw new NoSuchElementException("the iteration has no more elements");
					}
				}
				return element;
			}
			
		};
	}
	
}

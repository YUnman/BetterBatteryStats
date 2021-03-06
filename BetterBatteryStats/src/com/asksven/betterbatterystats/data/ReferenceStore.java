/*
 * Copyright (C) 2011 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asksven.betterbatterystats.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//import com.asksven.android.common.utils.DataStorage;

import android.content.Context;
import android.util.Log;

/**
 * @author sven
 * Static class as fassade to the cache of references
 */
public class ReferenceStore
{
	public static final String REF_UPDATED = "com.asksven.betterbatterystats.REF_UPDATED";
	
	/** the storage for references */
	private static  Map<String, Reference> m_refStore = new HashMap<String, Reference>();
	
	/** the logging tag */
	private static final String TAG = "ReferenceStore";

	/**
	 * Return the speaking reference names for references created after refFileName (or all if refFileName is empty or null)
	 * @param refName
	 * @param ctx
	 * @return
	 */
	public static List<String> getReferenceLabels(String refFileName, Context ctx)
	{
		List<String> ret = new ArrayList<String>();
		
		long time = 0;
		
		// if a ref name was passed get the time that ref was created
		if ((refFileName != null) && !refFileName.equals("") )
		{
			Reference ref = getReferenceByName(refFileName, ctx);
			if (ref != null)
			{
				time = ref.m_creationTime;
			}
		}
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		ret = db.fetchAllLabels(time);
		
		return ret;
	}

	/**
	 * Return the internal reference names for references created after refFileName (or all if refFileName is empty or null)
	 * @param refName
	 * @param ctx
	 * @return
	 */
	public static List<String> getReferenceNames(String refFileName, Context ctx)
	{
		List<String> ret = new ArrayList<String>();
		
		long time = 0;
		
		// if a ref name was passed get the time that ref was created
		if ((refFileName != null) && !refFileName.equals("") )
		{
			Reference ref = getReferenceByName(refFileName, ctx);
			if (ref != null)
			{
				time = ref.m_creationTime;
			}
		}
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		ret = db.fetchAllKeys(time);
		
		return ret;
	}

	/**
	 * Returns a reference given a name
	 * @param refName the name of the reference
	 * @param ctx
	 * @return
	 */
	public static Reference getReferenceByName(String refName, Context ctx)
	{
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);

//		if (!refName.startsWith("ref_"))
//		{
//			Log.e(TAG, "Invalid reference name " + refName);
//			return null;
//		}
		// the reference names are lazily loaded too
		if (m_refStore.keySet().isEmpty())
		{
			populateReferenceNames(ctx);
		}
		
		// we use lazy loading so we must check if there is a reference there
		if (m_refStore.get(refName) == null)
		{
			Reference thisRef = db.fetchReferenceByKey(refName);
			m_refStore.put(refName, thisRef);
		
			if (thisRef != null)
			{
				Log.i(TAG, "Retrieved reference from storage: " + thisRef.whoAmI());
			}
			else
			{
				Log.i(TAG, "Reference " + Reference.CUSTOM_REF_FILENAME
						+ " was not found");
			}
			
		}
		Reference ret = m_refStore.get(refName);
		return ret;
	}
	
	/**
	 * Adds a reference to the cache and persists it
	 * @param refName the name
	 * @param ref
	 * @param ctx
	 */
	public static synchronized void put(String refName, Reference ref, Context ctx)
	{
		m_refStore.put(refName, ref);
		Log.i(TAG, "Serializing reference " + refName);
		serializeRef(ref, ctx);
	}

	/**
	 * Invalidates a reference in the cache storage
	 * @param refName the name
	 * @param ref
	 * @param ctx
	 */
	public static void invalidate(String refName, Context ctx)
	{
		m_refStore.put(refName, null);
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		db.deleteReference(refName);
	}



	/**
	 * Returns whether a reference exists
	 * @param ref
	 * @param ctx
	 * @return
	 */
	public static boolean hasReferenceByName(String ref, Context ctx)
	{
		boolean ret = false;
		Reference myCheckRef = getReferenceByName(ref, ctx);

		if ((myCheckRef != null) && (myCheckRef.m_refKernelWakelocks != null))
		{
			ret = true;
		} else
		{
			ret = false;
		}

		return ret;
	}

	/**
	 * Marshalls a reference to storage
	 * @param refs
	 * @param ctx
	 */
	private static void serializeRef(Reference refs, Context ctx)
	{
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		db.addOrUpdateReference(refs);
//		DataStorage.objectToFile(ctx, refs.m_fileName, refs);
		Log.i(TAG, "Saved ref " + refs.m_fileName);
	}

	/**
	 * Unmarshalls all refs from storage
	 * @param ctx
	 */
	private static void deserializeAllRef(Context ctx)
	{
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		
		List<Reference> refs = db.fetchAllRows();
		for (int i = 0; i < refs.size(); i++)
		{
			
			Reference ref = refs.get(i);
			if (ref != null)
			{
				Log.i(TAG, "Retrieved ref: " + ref.whoAmI());
				m_refStore.put(ref.m_fileName, ref);
		
			}
			else
			{
				Log.i(TAG, "An error occured reading refs from the database");
			}
		}
	}

	/**
	 * Fill the cache names of all existing refernces
	 * @param ctx
	 */
	private static void populateReferenceNames(Context ctx)
	{
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		List<String> refs = db.fetchAllKeys(0);
		for (int i=0; i < refs.size(); i++)
		{
			m_refStore.put(refs.get(i), null);		
		}
	}
	
	/**
	 * Deletes (nulls) all serialized refererences
	 * @param ctx
	 */
	public static void deleteAllRefs(Context ctx)
	{
		ReferenceDBHelper db = ReferenceDBHelper.getInstance(ctx);
		db.deleteReferences();
	}
	
	public static void logReferences(Context ctx)
	{
		ReferenceDBHelper.getInstance(ctx).logCacheContent();
		
	}
}


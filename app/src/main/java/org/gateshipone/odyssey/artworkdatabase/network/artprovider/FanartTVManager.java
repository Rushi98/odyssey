/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.artworkdatabase.network.artprovider;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistFetchError;
import org.gateshipone.odyssey.artworkdatabase.network.requests.OdysseyJsonObjectRequest;
import org.gateshipone.odyssey.artworkdatabase.network.LimitingRequestQueue;
import org.gateshipone.odyssey.artworkdatabase.network.requests.ArtistImageByteRequest;
import org.gateshipone.odyssey.artworkdatabase.network.responses.ArtistImageResponse;
import org.gateshipone.odyssey.models.ArtistModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FanartTVManager implements ArtistImageProvider {
    private static final String TAG = FanartTVManager.class.getSimpleName();

    private static final String MUSICBRAINZ_API_URL = "http://musicbrainz.org/ws/2";

    private static final String FANART_TV_API_URL = "http://webservice.fanart.tv/v3/music";

    private RequestQueue mRequestQueue;

    private static FanartTVManager mInstance;

    private static final String MUSICBRAINZ_FORMAT_JSON = "&fmt=json";

    private static final int MUSICBRAINZ_LIMIT_RESULT_COUNT = 1;
    private static final String MUSICBRAINZ_LIMIT_RESULT = "&limit=" + String.valueOf(MUSICBRAINZ_LIMIT_RESULT_COUNT);


    private static final String API_KEY = "c0cc5d1b6e807ce93e49d75e0e5d371b";

    private FanartTVManager(Context context) {
        mRequestQueue = LimitingRequestQueue.getInstance(context);
    }

    public static synchronized FanartTVManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FanartTVManager(context);
        }
        return mInstance;
    }


    private <T> void addToRequestQueue(Request<T> req) {
        mRequestQueue.add(req);
    }

    public void fetchArtistImage(final ArtistModel artist, final Context context, final Response.Listener<ArtistImageResponse> listener, final ArtistFetchError errorListener) {

        String artistURLName = Uri.encode(artist.getArtistName().replaceAll("/", " "));

        getArtists(artistURLName, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONArray artists;
                try {
                    artists = response.getJSONArray("artists");

                    if (!artists.isNull(0)) {
                        JSONObject artistObj = artists.getJSONObject(0);
                        final String artistMBID = artistObj.getString("id");

                        getArtistImageURL(artistMBID, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                JSONArray thumbImages;
                                try {
                                    thumbImages = response.getJSONArray("artistthumb");

                                    JSONObject firstThumbImage = thumbImages.getJSONObject(0);
                                    artist.setMBID(artistMBID);
                                    getArtistImage(firstThumbImage.getString("url"), artist, listener, new Response.ErrorListener() {
                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            errorListener.fetchVolleyError(artist, context, error);
                                        }
                                    });

                                } catch (JSONException e) {
                                    errorListener.fetchJSONException(artist, context, e);
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                errorListener.fetchVolleyError(artist, context, error);
                            }
                        });
                    }
                } catch (JSONException e) {
                    errorListener.fetchJSONException(artist, context, e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                errorListener.fetchVolleyError(artist, context, error);
            }
        });
    }

    private void getArtists(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(FanartTVManager.class.getSimpleName(), artistName);

        String url = MUSICBRAINZ_API_URL + "/" + "artist/?query=artist:" + artistName + MUSICBRAINZ_LIMIT_RESULT + MUSICBRAINZ_FORMAT_JSON;

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getArtistImageURL(String artistMBID, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(FanartTVManager.class.getSimpleName(), artistMBID);

        String url = FANART_TV_API_URL + "/" + artistMBID + "?api_key=" + API_KEY;

        OdysseyJsonObjectRequest jsonObjectRequest = new OdysseyJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getArtistImage(String url, ArtistModel artist, Response.Listener<ArtistImageResponse> listener, Response.ErrorListener errorListener) {
        Log.v(FanartTVManager.class.getSimpleName(), url);

        Request<ArtistImageResponse> byteResponse = new ArtistImageByteRequest(url, artist, listener, errorListener);

        addToRequestQueue(byteResponse);
    }

    @Override
    public void cancelAll() {
        mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

}
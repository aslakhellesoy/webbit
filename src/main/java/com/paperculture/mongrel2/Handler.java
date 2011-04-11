/* Copyright 2011 Armando Singer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.paperculture.mongrel2;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * Mongrel2 hander. All objects are deeply immutable and thread safe.
 * Accessors never return null, but may be empty if empty is valid
 * (Strings, byte[], collections...).
 *
 * @author Armando Singer (armando.singer at gmail dot com)
 */
public final class Handler {

  /** One context per jvm; see: http://zguide.zeromq.org/chapter:all#toc10 */
  private static final Context CTX = ZMQ.context(1);
  private static final Charset ASCII = Charset.forName("US-ASCII");
  
  public static Connection connection(String senderId, String subAddress, String pubAddress) {
    return new Connection(senderId, subAddress, pubAddress);
  }

  public static Connection connection(byte[] senderId, String subAddress, String pubAddress) {
    return new Connection(senderId, subAddress, pubAddress);
  }

  /** The mogrel2 request; instances are deeply immutable and thread safe */
  public static final class Request implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String sender, connId, path;
    private final Map<String, String> headers;
    private final Map<String, Object> jsonData;
    private final byte[] msg;
    private final int bodyFromIndex, bodyToIndex;

    /** @throws IllegalArgumentException if any request part is invalid */
    private Request(String sender, String connId, String path, String headers,
      byte[] msg, int bodyFromIndex, int bodyToIndex, boolean forceJson) {
      this.sender = checkNotNullOrEmpty(sender, "Invalid request, no sender.");
      this.connId = checkNotNullOrEmpty(connId, "Invalid request, no connId.");
      this.path = checkNotNullOrEmpty(path, "Invalid request, no path");
      this.headers = checkNotNull(parseHeaders(headers), "Headers may be empty but not null");
      this.msg = checkNotNull(msg, "Message may be emtpy but not null");
      this.bodyFromIndex = bodyFromIndex;
      this.bodyToIndex = bodyToIndex;
      this.jsonData = parseBodyIfJson(this.headers, msg, bodyFromIndex, bodyToIndex, forceJson);
    }

    /**
     * This is used for all 0mq message parsing. 0mq only sends and receives byte[],
     * so we take care not convert the message body to a String to prevent unnecessary
     * re-encoding and copying, since not all handlers expect String data.
     * 
     * Format: UUID ID PATH SIZE:HEADERS,SIZE:BODY,
     */
    public static Request parse(byte[] msg, boolean forceJson) {
        try {
            System.out.println("msg = " + new String(msg, "US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      String sender, connId, path, headers;
      sender = connId = path = headers = null; 
      int headerSizeOffset = 0;
      int bodySizeOffset = 0;
      boolean inPrefix = true;
      for (int i = 0; i < msg.length; i++) {
        if (inPrefix && msg[i] == ' ') {
          if (sender == null)
            sender = rangeString(msg, 0, i);
          else if (connId == null)
            connId = rangeString(msg, sender.length() + 1, i);
          else {
            path = rangeString(msg, sender.length() + connId.length() + 2, i);
            headerSizeOffset = i + 1;
            inPrefix = false;
          }
        } else if (msg[i] == ':') {
          if (headers == null) {
            final int[] offsetAndLen = offsetAndLength(msg, headerSizeOffset, i);
            headers = new String(msg, offsetAndLen[0], offsetAndLen[1], ASCII);
              System.out.println("headers = " + headers);
            bodySizeOffset = (offsetAndLen[0] + offsetAndLen[1]) + 1;
          } else {
            final int[] offsetAndLen = offsetAndLength(msg, bodySizeOffset, i);
            return new Request(sender, connId, path, headers, msg, offsetAndLen[0],
              offsetAndLen[0] + offsetAndLen[1], forceJson);
          }
        }
      }
      throw new IllegalArgumentException(
        "Message was not in the format: ID PATH SIZE:HEADERS,SIZE:BODY,");
    }

    private static int[] offsetAndLength(byte[] msg, int sizeOffset, int index) {
      final int offset = index + 1;
      final int length = Integer.parseInt(rangeString(msg, sizeOffset, index));
      final int commaOffset = offset + length;
      if (msg[commaOffset] != ',')
        throw new IllegalStateException("Netstring did not end in ','");
      return new int[] { offset, length };
    }

    private static String rangeString(byte[] msg, int offset, int index) {
      return new String(msg, offset, index - offset, ASCII);
    }

    public String getSender() { return sender; }
    public String getConnId() { return connId; }
    public String getPath() { return path; }
    public byte[] getBody() { return Arrays.copyOfRange(msg, bodyFromIndex, bodyToIndex); }
    public String getBodyAsString() {
      return new String(msg, bodyFromIndex, bodyToIndex - bodyFromIndex, ASCII);
    }
    /** @return header map - never null but may be empty */
    public Map<String, String> getHeaders() { return headers; }
    /** @return json map - never null but may be empty */
    public Map<String, Object> getData() { return jsonData; }
    public boolean isDisconnect() {
      return "JSON".equals(headers.get("METHOD")) && "disconnect".equals(headers.get("type"));
    }
    public boolean shouldClose() {
      return "close".equals(headers.get("connection")) || "HTTP/1.0".equals(headers.get("VERSION"));
    }

    @Override public String toString() {
      return "Request [sender=" + sender + ", connId=" + connId + ", path=" + path
        + ", headers=" + headers + ", body=" +  (bodyToIndex < 5000
          ? getBodyAsString() : getBodyAsString().substring(0, 5000)) +"]";
    }
    @Override public int hashCode() { return hash(msg); }
    @Override public boolean equals(Object o) {
      if (!(o instanceof Request)) return false;
      final Request that = (Request) o;
      return Arrays.equals(msg, that.msg);
    }

    /** 2 element array with the parsed value and the rest */
    private static String[] parseNetstring(String ns) {
      final String[] split = ns.split(":", 2);
      final int len = Integer.parseInt(split[0]);
      final String rest = split[1];
      if (rest == null || len < 0 || !rest.substring(len, len + 1).equals(","))
        throw new IllegalStateException("Netstring did not end in ','");
      return new String[] { rest.substring(0, len), rest.substring(len + 1, rest.length())};
    }
    
    private static Map<String, String> parseHeaders(String jsonHeaders) {
      try {
        final JSONObject jsonObject = new JSONObject(jsonHeaders);
        final Map<String, String> result = new HashMap<String, String>();
        final String[] keys = JSONObject.getNames(jsonObject);
        if (keys != null)
          for (final String key : keys) result.put(key, jsonObject.getString(key));
        return Collections.unmodifiableMap(result);
      } catch (final JSONException e) {
        throw new IllegalArgumentException("Invalid json for headers", e);
      }
    }

    private static Map<String, Object> parseBodyIfJson(Map<String, String> headers,
      byte[] msg, int bodyFromIndex, int bodyToIndex, boolean forceJson) {
      if (!"JSON".equals(headers.get("METHOD")) && !forceJson) return Collections.EMPTY_MAP;
      try {
        return parse(new JSONObject(new String(msg, bodyFromIndex, bodyToIndex - bodyFromIndex, ASCII)));
      } catch (final JSONException e) {
        throw new IllegalArgumentException("Body is not valid json", e);
      }
    }

    private static Map<String, Object> parse(JSONObject jsObject) throws JSONException {
      final String[] keys = JSONObject.getNames(jsObject);
      final Map<String, Object> result = new HashMap<String, Object>();
      if (keys == null || keys.length == 0) return Collections.EMPTY_MAP;
      for (final String key : keys) {
        final Object value = jsObject.get(key);
        if (value instanceof JSONArray) parse((JSONArray) value);
        else if (value instanceof JSONObject) parse((JSONObject) value);
        else result.put(key, value);
      }
      return Collections.unmodifiableMap(result);
    }

    private static List<Object> parse(JSONArray jsArray) throws JSONException {
      if (jsArray.length() == 0) return Collections.EMPTY_LIST;
      final List<Object> result = new ArrayList<Object>(); 
      for (int i = 0; i < jsArray.length(); i++) {
        final Object value = jsArray.get(i);
        if (value instanceof JSONArray) parse((JSONArray) value);
        else if (value instanceof JSONObject) parse((JSONObject) value);
        else result.add(value);
      }
      return Collections.unmodifiableList(result);
    }

  }

  /**
   * A Connection object manages the connection between your handler and a Mongrel2 server
   * (or servers). It can receive raw requests or JSON encoded requests whether from HTTP
   * or MSG request types, and it can send individual responses or batch responses either
   * raw or as JSON. It also has a way to encode HTTP responses for simplicity since
   * that'll be fairly common.
   * 
   * Instances are deeply immutable and thread safe.
   */
  public static final class Connection {
    
    private final byte[] senderId;
    private final String subAddress, pubAddress;
    private final Socket reqs, resp;

    /** @throws IllegalArgumentException if any param is null or empty */
    public Connection(String senderId, String subAddress, String pubAddress) {
      this(senderId.getBytes(ASCII), subAddress, pubAddress);
    }
    
    /**
     * Addresses are 0mq format, for example: tcp://127.0.0.1:9998
     * @throws IllegalArgumentException if any param is null or empty
     */
    private Connection(byte [] senderId, String subAddress, String pubAddress) {
      this.senderId = checkNotNullOrEmpty(senderId, "must specify a senderId");
      this.subAddress = checkNotNullOrEmpty(subAddress, "must specify a subAddress");
      this.pubAddress = checkNotNullOrEmpty(pubAddress, "must specify a pubAddress");
      this.reqs = CTX.socket(ZMQ.PULL);
      reqs.connect(subAddress);
      this.resp = CTX.socket(ZMQ.PUB);
      resp.connect(pubAddress);
      resp.setIdentity(senderId);
    }

    /** @return created mongrel2 Request from 0mq */
    public Request recv() { return Request.parse(reqs.recv(0), false); }

    /**
     * Same as {@link Connection#recv()}, but populates {@link Request#getData()}
     * w/ json data. Normally Request just does this if the METHOD is 'JSON' but
     * you can use this to force it for say HTTP requests.
     *
     * @see Request#getData()
     * @throws IllegalArgumentException if body is not valid json
     */
    public Request recvJson() { return Request.parse(reqs.recv(0), true); }

    /** Raw send to the given connection ID at the given uuid. */ 
    void send(String uuid, String connId, byte[] msg) {
      final byte[] checkedMsg = msg == null ? EMPTY_BYTE_ARRAY : msg;
      final byte[] header = (uuid + ' ' + connId.length() + ':' + connId + ", ")
        .getBytes(ASCII);
      resp.send(concat(header, checkedMsg), 0);
    }
    
    /** Reply based on the given Request object and message. */
    public void reply(Request req, byte[] msg) { send(req.sender, req.connId, msg); }
    public void reply(Request req, String msg) {
      send(req.sender, req.connId, msg.getBytes(ASCII));
    }
    /** Same as reply, but tries to convert data to JSON first. */
    public void replyJson(Request req, Map<String, Object> jsonData) {
      send(req.sender, req.connId, new JSONObject(jsonData).toString().getBytes(ASCII));
    }

    /**
     * Basic HTTP response mechanism which will take your body, any headers you've 
     * made, and encode them so that the browser gets them.
     */
    public void replyHttp(Request req, String body) { replyHttp(req, body, 200); }
    public void replyHttp(Request req, String body, int code) { replyHttp(req, body, code, "OK"); }
    public void replyHttp(Request req, String body, int code, String status) {
      replyHttp(req, body, code, status, Collections.EMPTY_MAP);
    }
    public void replyHttp(Request req, String body, int code, String status,
      Map<String, String> headers) {
      replyHttp(req, body.getBytes(ASCII), code, status, headers);
    }

    public void replyHttp(Request req, byte[] body) { replyHttp(req, body, 200); }
    public void replyHttp(Request req, byte[] body, int code) { replyHttp(req, body, code, "OK"); }
    public void replyHttp(Request req, byte[] body, int code, String status) {
      replyHttp(req, body, code, status, Collections.EMPTY_MAP);
    }
    public void replyHttp(Request req, byte[] body, int code, String status,
      Map<String, String> headers) {
      reply(req, httpResponse(body, code, status, headers));
    }

    /**
     * This lets you send a single message to many currently connected clients.
     * There's a MAX_IDENTS that you should not exceed, so chunk your targets as needed.
     * Each target will receive the message once by Mongrel2, but you don't have 
     * to loop which cuts down on reply volume.
     */
    public void deliver(String uuid, Iterable<String> connIds, byte[] msg) {
      send(uuid, join(connIds, " "), msg);
    }
    public void deliver(String uuid, Iterable<String> connIds, String msg) {
      send(uuid, join(connIds, " "), msg.getBytes(ASCII));
    }
    /** Same as {@link Connection#deliver(String, Iterable, byte[])}, but converts to JSON first. */
    public void deliverJson(String uuid, Iterable<String> connIds, Map<String, Object> jsonData) {
      deliver(uuid, connIds, new JSONObject(jsonData).toString().getBytes(ASCII));
    }

    /**
     * Same as deliver, but builds an HTTP response, which means, yes, you can 
     * reply to multiple connected clients waiting for an HTTP response from one
     * handler. Kinda cool.
     */
    public void deliverHttp(String uuid, Iterable<String> connIds, String body) {
      deliverHttp(uuid, connIds, body, 200);
    }
    public void deliverHttp(String uuid, Iterable<String> connIds, String body, int code) {
      deliverHttp(uuid, connIds, body, code, "OK");
    }
    public void deliverHttp(String uuid, Iterable<String> connIds, String body,
      int code, String status) {
      deliverHttp(uuid, connIds, body, code, status, Collections.EMPTY_MAP);
    }
    public void deliverHttp(String uuid, Iterable<String> connIds, String body,
      int code, String status, Map<String, String> headers) {
      final byte[] checkedBody = body == null ? EMPTY_BYTE_ARRAY : body.getBytes(ASCII);
      deliverHttp(uuid, connIds, checkedBody, code, status, headers);
    }

    public void deliverHttp(String uuid, Iterable<String> connIds, byte[] body) {
      deliverHttp(uuid, connIds, body, 200);
    }
    public void deliverHttp(String uuid, Iterable<String> connIds, byte[] body, int code) {
      deliverHttp(uuid, connIds, body, code, "OK");
    }
    public void deliverHttp(String uuid, Iterable<String> connIds, byte[] body,
      int code, String status) {
      deliverHttp(uuid, connIds, body, code, status, Collections.EMPTY_MAP);
    }
    public void deliverHttp(String uuid, Iterable<String> connIds, byte[] body,
      int code, String status, Map<String, String> headers) {
      deliver(uuid, connIds, httpResponse(body, code, status, headers));
    }

    /** Tells mongrel2 to explicitly close the HTTP connection. */
    public void close(Request req) { reply(req, EMPTY_BYTE_ARRAY); }

    /** Same as close but does it to a whole bunch of idents at a time. */
    public void deliverClose(String uuid, Iterable<String> connIds) {
      deliver(uuid, connIds, EMPTY_BYTE_ARRAY);
    }
    
    public byte[] getSenderId() { return Arrays.copyOf(senderId, senderId.length); }
    public String getSenderIdString() { return new String(senderId, ASCII); }
    public String getSubAddress() { return subAddress; }
    public String getPubAddress() { return pubAddress; }

    @Override public String toString() {
      return "Connection [senderId=" + Arrays.toString(senderId) + ", subAddress="
        + subAddress + ", pubAddress=" + pubAddress + "]";
    }
    @Override public int hashCode() {
      return hash(senderId, pubAddress, subAddress);
    }
    @Override public boolean equals(Object o) {
      if (!(o instanceof Connection)) return false;
      final Connection that = (Connection) o;
      return Arrays.equals(senderId, that.senderId) && eq(pubAddress, that.pubAddress)
        && eq(subAddress, that.subAddress);
    }

    private static String join(Iterable<?> c, String sep) {
      Iterator<?> i;
      if (c == null || (!(i = c.iterator()).hasNext())) return "";
      final StringBuilder result = new StringBuilder(String.valueOf(i.next()));
      while (i.hasNext()) result.append(sep).append(i.next());
      return result.toString();
    }

    private static byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /** implementation concatenates bytes arrays to prevent unnecessary copies and encoding */
    private static byte[] httpResponse(byte[] body, int code, String status,
      Map<String, String> headers) {
      final byte[] checkedBody = body == null ? EMPTY_BYTE_ARRAY : body;
      final Map<String, String> headersCopy = new LinkedHashMap<String, String>(headers);
      headersCopy.put("Content-Length", String.valueOf(checkedBody.length));
      final StringBuilder head = new StringBuilder("HTTP/1.1 ").append(code)
        .append(' ').append(status).append("\r\n");
      for (final Entry<String, String> header : headersCopy.entrySet())
        head.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
      head.append("\r\n");
      return concat(head.toString().getBytes(ASCII), checkedBody);
    }
  }

  private static byte[] concat(byte[]... arrays) {
    int length = 0;
    for (final byte[] array : arrays) length += array.length;
    final byte[] result = new byte[length];
    int pos = 0;
    for (final byte[] array : arrays) {
      System.arraycopy(array, 0, result, pos, array.length);
      pos += array.length;
    }
    return result;
  }

  private static <T> T checkNotNull(T ref, String errorMessage) {
    if (ref == null) throw new NullPointerException(errorMessage);
    return ref;
  }
  
  private static String checkNotNullOrEmpty(String ref, String errorMessage) {
    checkNotNull(ref, errorMessage);
    if (ref.isEmpty()) throw new IllegalArgumentException(errorMessage);
    return ref;
  }
  
  private static byte[] checkNotNullOrEmpty(byte[] ref, String errorMessage) {
    checkNotNull(ref, errorMessage);
    if (ref.length == 0) throw new IllegalArgumentException(errorMessage);
    return ref;
  }

  private static int hash(Object... objects) { return Arrays.deepHashCode(objects); }
  private static boolean eq(Object a, Object b) { return a == b || (a != null && a.equals(b)); }

}
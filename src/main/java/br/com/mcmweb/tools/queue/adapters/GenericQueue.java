package br.com.mcmweb.tools.queue.adapters;

import java.io.IOException;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import br.com.mcmweb.tools.queue.messages.MessageRequest;
import br.com.mcmweb.tools.queue.messages.MessageResponse;

public abstract class GenericQueue {

	protected String host;
	protected String login;
	protected String password;
	protected String queueName;
	protected static final ObjectMapper mapper;

	private static final Logger logger = Logger.getLogger(GenericQueue.class.getName());

	static {
		mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
	}

	public GenericQueue(String host, String login, String password, String queueName) throws Exception {
		this.host = host;
		this.login = login;
		this.password = password;
		this.queueName = queueName;
	}

	/**
	 * Connect to queue
	 * 
	 * @throws Exception
	 */
	public abstract void connect() throws Exception;

	/**
	 * Add message to queue
	 * 
	 * @param object
	 * @return
	 */
	public abstract boolean put(Object object);

	/**
	 * Remove message from queue
	 * 
	 * @param response
	 * @return
	 */
	public abstract boolean delete(MessageResponse response);

	/**
	 * Release message back to queue, in delaySeconds
	 * 
	 * @param response
	 * @param delaySeconds
	 * @return
	 */
	public abstract boolean release(MessageResponse response, Integer delaySeconds);

	/**
	 * Touch queue message to avoid timeouts
	 * 
	 * @param response
	 * @return
	 */
	public abstract boolean touch(MessageResponse response);

	/**
	 * Retrieve next queued message
	 * 
	 * @return
	 */
	public abstract MessageResponse getNext();

	/**
	 * Close queue connection
	 */
	public abstract void close();

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	/**
	 * Create MessageRequest object to wrap your object and serialize everything
	 * 
	 * @param object
	 *            to be queued
	 * @return string to be queued
	 */
	protected String serializeMessageBody(Object object) {
		String fullMessageBody = null;
		try {
			String body = mapper.writeValueAsString(object);
			MessageRequest messageRequest = new MessageRequest();
			messageRequest.setType(object.getClass().getCanonicalName());
			messageRequest.setBody(body);
			fullMessageBody = mapper.writeValueAsString(messageRequest);
		} catch (JsonGenerationException e) {
			logger.severe("Unable to generate json: " + e.getMessage());
		} catch (JsonMappingException e) {
			logger.severe("Unable to map json to class: " + e.getMessage());
		} catch (IOException e) {
			logger.severe("I/O Error: " + e.getMessage());
		}
		return fullMessageBody;
	}

	protected MessageResponse unserializeMessageBody(String id, String handle, Integer receivedCount, String body) {
		MessageResponse messageResponse = new MessageResponse();
		messageResponse.setId(id);

		if (handle != null) {
			messageResponse.setHandle(handle);
		} else {
			messageResponse.setHandle(id);
		}

		if (receivedCount == null) {
			receivedCount = 0;
		}
		messageResponse.setReceivedCount(receivedCount);

		if (body != null && !"".equals(body)) {
			try {
				MessageRequest messageRequest = mapper.readValue(body, MessageRequest.class);
				messageResponse.setType(messageRequest.getType());
				messageResponse.setObject(mapper.readValue(messageRequest.getBody(), Class.forName(messageRequest.getType())));
				return messageResponse;
			} catch (JsonGenerationException e) {
				logger.severe("Unable to generate json: " + e.getMessage());
			} catch (JsonMappingException e) {
				logger.severe("Unable to map json to class: " + e.getMessage());
			} catch (IOException e) {
				logger.severe("I/O Error: " + e.getMessage());
			} catch (ClassNotFoundException e) {
				logger.severe("Unable to unserialize class: " + e.getMessage());
			}
		}

		return null;
	}

}

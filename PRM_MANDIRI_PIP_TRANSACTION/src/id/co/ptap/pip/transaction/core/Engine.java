package id.co.ptap.pip.transaction.core;

import id.co.ptap.util.email.SendMailManager;
import id.co.ptap.util.pip.Bean;
import id.co.ptap.util.pip.SystemProperties;
import id.co.ptap.util.pip.XStreamContext;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;

public class Engine implements ParsingRecords {
	public Logger log = Logger.getLogger(Engine.class.getName());
	protected static Bean bean;
	@SuppressWarnings("unused")
	private static final String ENTER = "\r\n";
	@SuppressWarnings("unused")
	private static final String EXT_PROPERTY_URL = "transaction_debitedc.prop";
	private static SystemProperties prop = null;

	private static String XML;
	private static String PATH_READ_FILE;
	private static String PREFIX_READ_FILE;
	private static String PREFIX_PROCESS_FILE;
	private static String PREFIX_FINISH_FILE;
	private static String NEW_FILE_PATH;
	private static String NEW_FILE_PREFIX;
	private static String RECORD_LENGTH;
	private static String HOSTMANDIRI;
	private static String PORTMANDIRI;
	private static String SENDMANDIRI;
	private static String EMAILMANDIRI;

	private int count;

	public String dtdName = "";
	public String subject = "";
	public String msg = "";
	public String fileName = "";
	public String[] emailTo;
	private BufferedReader in;
	private BufferedWriter out;
	SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	public Engine() {
		prop = SystemProperties.getInstance();
		try {
			prop.loadProperties(System.getProperty("XML"));
		} catch (IOException e) {
			this.msg = ("Error - Problem Load Properties" + e.getMessage());
			this.log.error(this.msg, e);
			System.exit(1);
		}
	}

	private void loadMail() {
		this.log.info("Starting Load SMTP (Email) Configuration");

		HOSTMANDIRI = prop.getProperty("mail.smtp.host");
		PORTMANDIRI = prop.getProperty("mail.smtp.port");
		SENDMANDIRI = prop.getProperty("mail.default.sender");
		EMAILMANDIRI = prop.getProperty("mail.default.receiver");
	}

	private void loadConfig() throws Exception {
		this.log.info("Starting Load Properties Configuration");

		XML = prop.getProperty("XML");
		PATH_READ_FILE = prop.getProperty("PATH_READ_FILE").replaceAll("/",
				"//");
		NEW_FILE_PATH = prop.getProperty("NEW_FILE_PATH").replaceAll("/", "//");
		PREFIX_READ_FILE = prop.getProperty("PREFIX_READ_FILE");
		PREFIX_PROCESS_FILE = prop.getProperty("PREFIX_PROCESS_FILE");
		PREFIX_FINISH_FILE = prop.getProperty("PREFIX_FINISH_FILE");
		NEW_FILE_PREFIX = prop.getProperty("NEW_FILE_PREFIX");
		RECORD_LENGTH = prop.getProperty("RECORD_LENGTH");
	}

	private void loadXML() {
		this.log.info("Starting Load XML Configuration");
		try {
			XStreamContext.setName(this.dtdName);
			bean = (Bean) XStreamContext.marshallXStream().fromXML(
					new FileInputStream(XML));
		} catch (Exception e) {
			this.msg = ("Error - Load XML Configuration Problem " + e
					.getMessage());
			this.log.error(this.msg);
			System.exit(1);
		}
	}

	private void proccess() throws IOException {
		String prefixFile = "";

		File srcFile = new File(PATH_READ_FILE);
		for (File tempFile : srcFile.listFiles())
			if (tempFile.isFile()) {
				this.fileName = tempFile.toString();

				if (tempFile.getName().startsWith(PREFIX_READ_FILE)) {
					prefixFile = renamingFile(tempFile.getParent(),
							tempFile.getName(), PREFIX_PROCESS_FILE);

					if (prefixFile.startsWith(PREFIX_PROCESS_FILE)) {
						long timestart = System.currentTimeMillis();

						proccessFile(tempFile.getParent(), prefixFile);

						renamingFile(tempFile.getParent(), prefixFile,
								PREFIX_FINISH_FILE);

						long time = System.currentTimeMillis() - timestart;
						this.log.info("FINISH PROCESS FILE : {" + this.fileName
								+ "} WITH TOTAL TAKE TIME : " + time
								+ " ms, TOTAL RECORDS : " + this.count
								+ " rec.");
					}
				}
			}
	}

	private void proccessFile(String path, String fileProcess) {
		this.count = 0;
		String line = "";
		try {
			File fileOut = new File(NEW_FILE_PATH + "/" + NEW_FILE_PREFIX
					+ fileProcess.substring(1));

			this.in = new BufferedReader(new FileReader(path + "/"
					+ fileProcess));
			this.out = new BufferedWriter(new FileWriter(fileOut.getPath()));

			this.log.info("Starting process file : {" + this.fileName + "}");

			while ((line = this.in.readLine()) != null) {
				StringBuffer buffer = new StringBuffer();
				String str = parsingRecords(line, RECORD_LENGTH);
				if (str.length() > 0) {
					buffer.append(str);

					this.out.write(buffer.toString() + "\r\n");
					this.out.flush();
				}
				this.count += 1;
			}

			this.out.close();
			this.in.close();
		} catch (Exception e) {
			this.msg = ("Process Create File Problem Exception ===> " + e
					.getMessage());
			this.log.error(this.msg, e);
			sendEmail();
		}
	}

	private String renamingFile(String path, String oldName, String action) {
		File file = new File(path + "/" + oldName);
		File file2 = new File(path + "/" + action + oldName);

		boolean success = file.renameTo(file2);
		if (!success) {
			this.log.error("File {" + this.fileName
					+ "} ==> was not successfully renamed");
			sendEmail();
		}
		return file2.getName();
	}

	public void run() {
		try {
			loadMail();
		} catch (Exception e) {
			this.msg = ("Error - SMTP (Email) Configuration Problem " + e
					.getMessage());
			this.log.error(this.msg);

			System.exit(1);
		}
		try {
			loadConfig();
		} catch (Exception e) {
			this.msg = ("Error - Load Properties Configuration Problem " + e
					.getMessage());
			this.log.error(this.msg);

			System.exit(1);
		}

		loadXML();

		boolean running = true;
		while (running) {
			try {
				proccess();
			} catch (IOException e) {
				this.msg = "Error - Problem Process File";
				this.log.error(this.msg, e);
			}
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
				this.msg = "Eror - Sleep Thread Problem";
				this.log.error(this.msg, e);
			}
		}
	}

	public String parsingRecords(String line, String record_length) {
		return null;
	}

	public void setDtdName(String dtdName) {
		this.dtdName = dtdName;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public void setEmailTo(String[] emailTo) {
		this.emailTo = emailTo;
	}

	public void sendEmail() {
		String[] split = EMAILMANDIRI.split(",");
		this.emailTo = new String[split.length];

		for (int i = 0; i < split.length; i++) {
			this.emailTo[i] = split[i];
		}
		try {
			SendMailManager.getInstance(HOSTMANDIRI, PORTMANDIRI, SENDMANDIRI)
					.SendMessageByMandiri(this.emailTo, this.subject, this.msg);
		} catch (MessagingException e) {
			this.msg = ("Error - Load SMTP (EMAIL) Configuration Problem " + e
					.getMessage());
			this.log.error(this.msg);

			// System.exit(1);
		}
	}
}

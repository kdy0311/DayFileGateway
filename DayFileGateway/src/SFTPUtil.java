import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SFTPUtil {
	private Session session = null;
	private Channel channel = null;
	private ChannelSftp channelSftp = null;

	// SFTP 서버연결
	public boolean init(String url, String user, int port, String password) {
		System.out.println(url);
		boolean result = true;
		// JSch 객체 생성
		JSch jsch = new JSch();
		try {
			// 세션객체 생성 ( user , host, port )
			session = jsch.getSession(user, url, port);
			// password 설정
			session.setPassword(password);
			// 세션관련 설정정보 설정
			java.util.Properties config = new java.util.Properties();
			// 호스트 정보 검사하지 않는다.
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			// 접속
			session.connect();
			// sftp 채널 접속
			channel = session.openChannel("sftp");
			channel.connect();
		} catch (JSchException e) {
			e.printStackTrace();
			result = false;
		}
		channelSftp = (ChannelSftp) channel;
		return result;
	}

	/**
	 * 단일 파일을 업로드
	 * 
	 * @param dir
	 *            저장시킬 주소(서버)
	 * @param file
	 *            저장할 파일 경로
	 */
	public boolean upload(String dir, String filePath) {
		boolean result = true;
		FileInputStream in = null;
		try {
			File file = new File(filePath);
			String fileName = file.getName();
			System.out.println("fileName:>>" + fileName);
			// fileName = URLEncoder.encode(fileName,"EUC-KR");
			in = new FileInputStream(file);
			channelSftp.cd(dir);
			channelSftp.put(in, fileName);
		} catch (Exception e) {
			e.printStackTrace();
			result = false;
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * 단일 파일 다운로드
	 * 
	 * @param dir
	 *            저장할 경로(서버)
	 * @param downloadFileName
	 *            다운로드할 파일
	 * @param path
	 *            저장될 공간
	 */
	public boolean download(String dir, String fileName, String copyDir) {
		boolean result = true;
		File copyFolder = new File(copyDir);
		try {
			channelSftp.cd(dir);
			Vector<LsEntry> files = channelSftp.ls("*"+fileName+"*");
			if (!copyFolder.exists()) {
				copyFolder.mkdir(); // 폴더 생성
				System.out.println("폴더가 생성되었습니다.");
			} else {
				System.out.println("이미 폴더가 생성되어 있습니다.");
			}
			for (ChannelSftp.LsEntry file : files) {
				File temp = new File(copyFolder.getAbsolutePath() + File.separator + file.getFilename());
				InputStream fis = null;
				OutputStream fos = null;
				try {
					fis = channelSftp.get(file.getFilename());
					fos = new FileOutputStream(temp);
					byte[] b = new byte[4096];
					int cnt = 0;
					while ((cnt = fis.read(b)) != -1) {
						fos.write(b, 0, cnt);
					}
					System.out.println("::>> "+file.getFilename()+" 다운로드 완료.");
				} catch (Exception e) {
					result = false;
					e.printStackTrace();
					System.out.println("::>> "+file.getFilename()+" 다운로드 실패.");
				} finally {
					try {
						fis.close();
						fos.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
		} catch (SftpException e) {
			result = false;
			e.printStackTrace();
		}
		
		return result;
	}
	public boolean remove(String dir, String downloadFileName, String path) {
		boolean result = true;
		try {
			channelSftp.rm(downloadFileName);
		} catch (SftpException e) {
			result = false;
			e.printStackTrace();
		}
		return result;
	}

	// 파일서버와 세션 종료
	public boolean disconnect() {
		channelSftp.quit();
		session.disconnect();
		
		return true;
	}
}

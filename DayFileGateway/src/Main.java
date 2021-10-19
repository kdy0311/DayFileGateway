import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author kdy
 * 2021-04-08
 * 농공산단, 하이옥스에 있는 데이터를 사업개발실로 하루마다 옮김.
 * 농공산단->운영서버->사업개발실
 * 하이옥스->운영서버->사업개발실
 *
 */
public class Main {
	
	private static final Logger LOG = Logger.getGlobal();
	private static final String NG_URL = "220.80.195.130";				// 농공산단 URL
    private static final String NG_USER = "itman";						// 농공산단 계정
    private static final int NG_PORT = 1122;							// 농공산단 port
    private static final String NG_PW = "itman0808!";					// 농공산단 비밀번호
    private static final String NG_PATH = "/data/ifs";					// 농공산단 디렉토리(서버)
    private static final String HI_URL = "hiox-pms.iptime.org";			// 하이옥스 URL
    private static final String HI_USER = "pi";							// 하이옥스 계정
    private static final int HI_PORT = 22;								// 하이옥스 port
    private static final String HI_PW = "itman0808!";					// 하이옥스 비밀번호
    private static final String HI_PATH = "/home/pi/IDP_HIOX/isdp";		// 하이옥스 디렉토리(서버)
    private static final String BD_URL = "192.168.0.4";					// 사업개발실 URL
    private static final String BD_USER = "ehdud0311";					// 사업개발실 계정
    private static final int BD_PORT = 2121;							// 사업개발실 port
    private static final String BD_PW = "itman1234!@";					// 사업개발실 비밀번호
    private static final String BD_NG_PATH = "/NASHDD/12_EMS2_PMS/수집데이터/농공산단";	// 사업개발실 디렉토리(서버)
    private static final String BD_HI_PATH = "/NASHDD/12_EMS2_PMS/수집데이터/HIOX";		// 사업개발실 디렉토리(서버)
    
    public static void main(String[] args) throws SecurityException, IOException {
    	// 로그 설정 start
		Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        if (handlers[0] instanceof ConsoleHandler) {
            rootLogger.removeHandler(handlers[0]);
        }
        LOG.setLevel(Level.INFO);
        Handler handler = new FileHandler("/home/Oadr/dayFileGateway.log", true);
        CustomLogFormatter formatter = new CustomLogFormatter();
        handler.setFormatter(formatter);
        LOG.addHandler(handler);
        // 로그 설정 end
        
        downloadFile(NG_URL, NG_USER, NG_PORT, NG_PW, NG_PATH, "_ng", BD_NG_PATH);
        downloadFile(HI_URL, HI_USER, HI_PORT, HI_PW, HI_PATH, "_hi", BD_HI_PATH);
        
    }
    public static void downloadFile(String url, String user, int port, String pw, 
    		String path, String id, String serverPath){
    	Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy_MM");
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy_MMdd");
		cal.set(Calendar.DATE, cal.get(Calendar.DATE) - 1);
		String folderName = sdf1.format(cal.getTime());
		String fileName = sdf2.format(cal.getTime());
		String localPath = System.getProperty("user.dir");
		SFTPUtil sftpUtil = new SFTPUtil();
        FTPUploader ftpUploader = new FTPUploader(BD_URL, BD_PORT, BD_USER, BD_PW);
        String dir = path+"/"+folderName;				// 원본 폴더
        String copyDir = localPath+"/"+folderName+id;	// 복사 폴더
        String bdCopyDir = serverPath+"/"+folderName+id;// 사업개발실 복사 폴더
        
    	if(sftpUtil.init(url, user, port, pw)){
        	LOG.info("::>> "+id+" 연결 성공");
        }else{
        	LOG.info("::>> "+id+" 연결 실패");
        	while(!sftpUtil.init(url, user, port, pw));
        }
        if(sftpUtil.download(dir, fileName, copyDir)){
        	LOG.info("::>> "+id+" 데이터 다운로드 완료");
        }else{
        	LOG.info("::>> "+id+" 데이터 다운로드 실패");
        }
        if(sftpUtil.disconnect()){
        	LOG.info("::>> "+id+" 연결 종료");
        }
        try {
			if(ftpUploader.upload(copyDir, bdCopyDir)){
				LOG.info("::>> 사업개발실 데이터 전송 완료");
				deleteFile(copyDir);
			}else{
				LOG.info("::>> 사업개발실 데이터 전송 실패");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    private static void deleteFile(String folderPath) {
		File folder = new File(folderPath);
		if( folder.exists() ){ 
			File[] folder_list = folder.listFiles();	// 파일리스트 얻어오기
			for (int i = 0; i < folder_list.length; i++) {
				if (folder_list[i].delete()) {// 파일 삭제
					LOG.info("::>> 운영서버 파일 삭제 성공 ==>> " + folder_list[i].getName());
					System.out.println("파일 삭제 성공");
				} else {
					LOG.info("::>> 운영서버 파일 삭제 실패 ==>> " + folder_list[i].getName());
					System.out.println("파일 삭제 실패");
				}
			}
			if(folder.isDirectory()){ 
				folder.delete(); //대상폴더 삭제
				LOG.info("::>> 운영서버 폴더삭제 성공 ==>> "+folder.getName());
				System.out.println("폴더가 삭제되었습니다.");
			}else{
				LOG.info("::>> 운영서버 폴더삭제 실패 ==>> "+folder.getName());
				System.out.println("운영서버 폴더삭제 실패"); 
			} 
		}else{ 
			LOG.info("::>> 운영서버 폴더 없음");
			System.out.println("운영서버 폴더 없음"); 
		}
		
	}
}
package com.github.novicezk.midjourney.util;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.PutObjectResult;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author wangding
 * @date 2023年8月25日 下午11:52:11
 * 阿里云oss文件上传
 */
@Component
@Slf4j
public class AliyunOssUtil {

	/**图片上传信息*/
	@Value("${aliyun.oss.accesskeyid}")
	private String imgAccessKeyId;
	@Value("${aliyun.oss.accesskeysecret}")
	private String imgAccessKeySecret;
	/**上传服务器所在点*/
 	private static String endpoint = "oss-cn-shenzhen.aliyuncs.com";
 	/**存储名字*/
    private static String bucketName = "foolian";
 	/**上传图片基本域名*/
    private static String baseImgDomain = "https://cdn.chuanxi.fun";
    
    /**
     * 流文件上传
     * 
     * @param inputStream
     * @param pathname 文件路径 /chat/hah/pic.png
     */
    public void uploadFileByStream(InputStream inputStream,String pathname) {
    	// 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, imgAccessKeyId, imgAccessKeySecret);
        try {
        	String finalPath = baseImgDomain + "/" + pathname;
            // 创建PutObject请求。
        	PutObjectResult putObjectResult = ossClient.putObject(bucketName, pathname, inputStream);
        	log.info(finalPath);
        } catch (OSSException oe) {
        	log.error("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.", oe);
        	log.error("Error Message:" + oe.getErrorMessage());
        	log.error("Error Code:" + oe.getErrorCode());
        	log.error("Request ID:" + oe.getRequestId());
        	log.error("Host ID:" + oe.getHostId());
			return ;
        } catch (ClientException ce) {
        	log.error("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
        	log.error("Error Message:" + ce.getMessage());
			return ;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return ;
	}
    
    /**
     * 判断文件是否存在
     * 
     * @param path
     * @return
     */
    public static boolean existRemoteFile(String path) {
		try {
			path = baseImgDomain + "/" + path;
			URL url = new URL(path);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("HEAD");
			//设置2000
			connection.setConnectTimeout(2000);
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				// 文件存在
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
    }
    
    /**
     * 判断是否存在
     * Administrator
     * @param path
     */
    public boolean existAliyunRemoteFile(String path) {
        // 填写Object完整路径，例如exampledir/exampleobject.txt。Object完整路径中不能包含Bucket名称。
        int index = path.indexOf("/", 10);
		String objectName = path.substring(index + 1);
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, imgAccessKeyId, imgAccessKeySecret);
        try {
            // ossObject包含文件所在的存储空间名称、文件名称、文件元信息以及一个输入流。           
            boolean flag = ossClient.doesObjectExist(bucketName, objectName);
            return flag;
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
            return false;
        } catch (Throwable ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * 读取文件的流
     * Administrator
     * @param filePath
     * @return
     */
    public static InputStream getInputStream(String filePath) {
        try {
			URL url = new URL(filePath);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(6000);
			urlConnection.setReadTimeout(6000);
			if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return null;
			}
			InputStream inputStream = urlConnection.getInputStream();
			return inputStream;
		} catch (Exception e) {
			return null;
		}
    }
    
    /**
     * 后台上传文件
     * @param urlList
     */
    public void uploadImgList(List<String> urlList) {

    	//开启子线程跑
    	new Thread() {
    		public void run() {
    	    	//过滤url
    	    	List<String> finalUrlList = new ArrayList<String>();
    	    	for(String url: urlList) {
    	    		boolean flag = existAliyunRemoteFile(url);
    	    		if(!flag) {
    	    			finalUrlList.add(url);
    	    		}
    	    	}
    	    	
    			if(finalUrlList != null && finalUrlList.size() > 0) {
    				for(String path:finalUrlList) {
    					int index = path.indexOf("/", 10);
        				String filePath = path.substring(index + 1);        				
        				InputStream inputStream = getInputStream(path);
        				uploadFileByStream(inputStream, filePath);	
    				}
    			}
    		};
    	}.start();
    }
    
    /**
     * 阻塞上传文件
     * Administrator
     * @param url
     */
    public void uploadImg(String url) {
		boolean flag = existAliyunRemoteFile(url);
		if (flag) {
			return;
		}
		int index = url.indexOf("/", 10);
		String filePath = url.substring(index + 1);
		InputStream inputStream = getInputStream(url);
		uploadFileByStream(inputStream, filePath);
    }
    
}

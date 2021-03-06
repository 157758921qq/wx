package com.markus.wx.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletInputStream;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Service;

import com.markus.wx.config.WXConfig;
import com.markus.wx.replymessage.entity.Article;
import com.markus.wx.replymessage.entity.BaseMessage;
import com.markus.wx.replymessage.entity.Image;
import com.markus.wx.replymessage.entity.ImageMessage;
import com.markus.wx.replymessage.entity.MusicMessage;
import com.markus.wx.replymessage.entity.NewsMessage;
import com.markus.wx.replymessage.entity.TextMessage;
import com.markus.wx.replymessage.entity.VideoMessage;
import com.markus.wx.replymessage.entity.VoiceMessage;
import com.markus.wx.token.entity.AccessToken;
import com.markus.wx.util.Util;
import com.thoughtworks.xstream.XStream;

import net.sf.json.JSONObject;

@Service
public class WXService {
	

	

	/**
	 * 根据微信服务器发送过来的值，进行验证
	 * 
	 * @param timestamp
	 * @param nonce
	 * @param signature
	 * @return
	 */
	public boolean check(String timestamp, String nonce, String signature) {
		// 1、将token 、timestamp、 nonce 三个参数字典排序
		String[] strs = new String[] { WXConfig.TOKEN, timestamp, nonce };
		Arrays.sort(strs);
		// 2将三个参数字符串拼接成一个字符串，再加密
		String str = strs[0] + strs[1] + strs[2];
		String mysig = sha1(str);
		// System.out.println(mysig.toString());
		// System.out.println(signature.toString());
		return mysig.equals(signature);
	}

	// sha加密
	private String sha1(String str) {
		try {
			// 获取加密对象
			MessageDigest md = MessageDigest.getInstance("sha1"); // md5
			// 加密
			byte[] digest = md.digest(str.getBytes());
			char[] chars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
			StringBuilder sb = new StringBuilder();
			// 处理加密结果
			for (byte b : digest) {
				sb.append(chars[(b >> 4) & 15]);
				sb.append(chars[b & 15]);
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	
	
	/**
	 * 	从输入流中 将xml 转成 map 格式
	 * 	Dom4j类
	 * @param inputStream
	 * @return
	 */
	public Map<String, String> fromRequsetParseXMLToMap(ServletInputStream is) {
		Map<String, String> map = new HashMap<String, String>();
		SAXReader reader = new SAXReader();
		try {
			// 读取输入流获取文档对象
			Document document = reader.read(is);
			// 根据文档对象获取根节点
			Element root = document.getRootElement();
			List<Element> elements = root.elements();
			for (Element e : elements) {
				map.put(e.getName(), e.getStringValue());
			}
		} catch (DocumentException e) {
			e.printStackTrace();
		}
		return map;
	}

	
	/**
	 * 	从requestMap中获取MsgType
	 * 	根据接收的消息MsgType类型，进行处理或回复
	 * 	接收到的消息的MsgType类型有：
	 * 	接收到的普通消息：文本消息text 、图片消息image、语音消息voice、
	 * 		       视频消息video、 小视频消息shortvideo、地理位置location
	 * 		        链接消息link
	 * 
	 * 	接收到的事件推送：  MsgType	消息类型，event
	 * 			关注或取消              	Event 事件类型，subscribe(订阅)、unsubscribe(取消订阅)
	 * 
	 * 			扫码带参数二维码    	Event	事件类型，subscribe
	 * 			上报地理位置           	Event	事件类型，LOCATION
	 * 	  	
	 * 			自定义菜单			Event	事件类型，CLICK
	 * 						    EventKey	事件KEY值，与自定义菜单接口中KEY值对应
	 * 
	 * 			点击菜单拉取消息时       Event	事件类型，CLICK
	 * 			点击菜单跳转 		Event	事件类型，VIEW
	 * 
	 * 
	 * 		回复处理
	 * 		1、文本  text
	 * 		2、图片  image
	 * 		3、语音  voice
	 * 		4、视频  video
	 * 		5、音乐  music
	 * 		6、图文  news
	 * @param requestMap
	 * @return
	 */
	public String replyMessageToUser(Map<String, String> requestMap) {
		BaseMessage msg = null;
		String msgType = requestMap.get("MsgType");
		// 根据不同的消息类型做相应的处理
		switch (msgType) {
		case "text":
			// 根据不同的消息类型，开始处理具体的消息
			msg = dealTextMessage(requestMap);
			break;
		case "image":
			msg = dealImgMessage(requestMap);
			break;
		case "voice":
			break;
		case "video":
			break;
		case "music":
			break;
		case "news":
			break;
		}
		// System.out.println(msg);
		if (msg != null) {
			//将对象转成XML
			return objectToXml(msg);
		}
		return null;
	}

	//回复一条Text文本类型的消息
	private BaseMessage dealTextMessage(Map<String, String> requestMap) {
		//根据text文本消息的内容做相对应的处理
		String msgContent = requestMap.get("Content");
		if(msgContent.equals("登录")) {
			//引导到网页授权登录页面
			//这个redirct_uri是用户点击后跳转到的 uri
		
			String url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx98b0a5b92b436370"
					+ "&redirect_uri=http://yzcoder.nat300.top/wx/auth?uri=http://yzcoder.nat300.top/wx/login"
					+ "&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
			
			TextMessage tm = new TextMessage(requestMap, "<a href=\""+url+"\">点击登录</a>");
			return tm;	
		} else {
			//url 点这个图文到哪去？
			//picUrl  图片地址
		//http://mmbiz.qpic.cn/mmbiz_jpg/SiaRSn312IPaGibk2bicQpI8SuaJdcC4xlIwID1wRszvwsghUCfm9N7IcXdJ4dCrnWc2jM2Ps45yFlws8Zk75LhDw/0?wx_fmt=jpeg
			
			//回复图文消息
			List<Article> articles = new ArrayList<Article>();
			Article article = new Article("热爱编程", "代码是生命的一部分，一天不碰键盘，手就痒，心就不得安宁!", "http://mmbiz.qpic.cn/mmbiz_jpg/SiaRSn312IPaGibk2bicQpI8SuaJdcC4xlIwID1wRszvwsghUCfm9N7IcXdJ4dCrnWc2jM2Ps45yFlws8Zk75LhDw/0?wx_fmt=jpeg", "");
			articles.add(article);
			NewsMessage news = new NewsMessage(requestMap, articles);
			return news;
		}
	
	}
	
	private BaseMessage dealImgMessage(Map<String, String> requestMap) {
		Image img = new Image("6bjdeUIo8prvpGCwgeqILi39zSWTn1PkJB5Ig6DcWJw");
		ImageMessage im = new ImageMessage(requestMap, img);
		return im;
	}
	

	private String objectToXml(BaseMessage msg) {
		XStream stream = new XStream();
		// 设置需要处理的XStreamAlias("xml")注释的类
		stream.processAnnotations(TextMessage.class);
		stream.processAnnotations(ImageMessage.class);
		stream.processAnnotations(MusicMessage.class);
		stream.processAnnotations(NewsMessage.class);
		stream.processAnnotations(VideoMessage.class);
		stream.processAnnotations(VoiceMessage.class);
		String xml = stream.toXML(msg);
		return xml;
	}
	
	
	
	
}

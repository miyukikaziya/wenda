package com.nowcoder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WendaApplication {

	public static void main(String[] args) {
		//User user2 = JSON.parseObject(value, User.class);//反序列化
		//System.out.println("47---------------" + user2);

		SpringApplication.run(WendaApplication.class, args);
	}
}

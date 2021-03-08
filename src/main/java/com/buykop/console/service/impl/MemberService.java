package com.buykop.console.service.impl;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.buykop.console.entity.PMember;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.oauth2.PRMemberType;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseService;



@Service
@Component
public class MemberService  extends BaseService implements com.buykop.console.service.MemberService{

	@Autowired
	private AmqpTemplate rabbitTemplate;

	public AmqpTemplate getRabbitTemplate() {
		return rabbitTemplate;
	}
	
	
	public void cancel(String memberId,UserToken token) throws Exception{
		
		
		if(DataChange.isEmpty(memberId)) return;
		

		PUserMember uo=new PUserMember();
		uo.setMemberId(memberId);
		this.delete(uo,token,true);
		
		
		PMember member=new PMember();
		member.setMemberId(memberId);
		this.delete(member,token,true);
		
		
		PRMemberType rmt=new PRMemberType();
		rmt.setMemberId(memberId);
		this.delete(rmt,token,true);
		
	}
	

}

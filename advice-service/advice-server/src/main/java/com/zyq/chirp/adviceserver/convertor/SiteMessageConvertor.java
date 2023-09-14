package com.zyq.chirp.adviceserver.convertor;

import com.zyq.chirp.adviceclient.dto.SiteMessageDto;
import com.zyq.chirp.adviceserver.domain.MessageType;
import com.zyq.chirp.chirpclient.dto.LikeDto;

import java.util.Optional;

public class SiteMessageConvertor {
    /*  public static InteractionMessage convertByLikeDto(LikeDto likeDto){
          InteractionMessage message=new InteractionMessage();
          Optional.ofNullable(likeDto)
                  .ifPresent(like->{
                      message.setSenderId(like.getUserId());
                      message.setChirperId(like.getChirperId());
                      message.setText(MessageType.LIKE.toString());
                  });
          return message;
      }*/
    public static SiteMessageDto convertByLikeDto(LikeDto likeDto) {
        SiteMessageDto message = new SiteMessageDto();
        Optional.ofNullable(likeDto)
                .ifPresent(like -> {
                    message.setSenderId(like.getUserId());
                    message.setTargetId(like.getChirperId());
                    message.setType(MessageType.LIKE.toString());
                });
        return message;
    }
}

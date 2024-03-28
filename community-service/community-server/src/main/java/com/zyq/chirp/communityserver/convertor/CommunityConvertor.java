package com.zyq.chirp.communityserver.convertor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyq.chirp.communityclient.dto.CommunityDto;
import com.zyq.chirp.communityclient.dto.RuleDto;
import com.zyq.chirp.communityserver.domain.pojo.Community;
import lombok.SneakyThrows;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommunityConvertor {
    ObjectMapper mapper = new ObjectMapper();

    @Mapping(target = "rules", source = "rules", qualifiedByName = "dtoRulesToPojoRules")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "dtoTagsToPojoTags")
    Community dtoToPojo(CommunityDto communityDto);

    @Mapping(target = "rules", source = "rules", qualifiedByName = "pojoRulesToDtoRules")
    @Mapping(target = "tags", source = "tags", qualifiedByName = "pojoTagsToDtoTags")
    CommunityDto pojoToDto(Community community);

    @SneakyThrows
    @Named("dtoRulesToPojoRules")
    default String getRules(List<RuleDto> ruleDtos) {
        return mapper.writeValueAsString(ruleDtos);
    }

    @SneakyThrows
    @Named("pojoRulesToDtoRules")
    default List<RuleDto> getRules(String rules) {
        return mapper.readValue(rules, new TypeReference<>() {
        });
    }

    @SneakyThrows
    @Named("dtoTagsToPojoTags")
    default String getTags(List<String> tags) {
        return mapper.writeValueAsString(tags);
    }

    @SneakyThrows
    @Named("pojoTagsToDtoTags")
    default List<String> getTags(String tags) {
        return mapper.readValue(tags, new TypeReference<>() {
        });
    }
}

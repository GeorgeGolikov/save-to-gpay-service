package com.example.savetogpay.gpay.strategy.concrete;

import com.example.savetogpay.dto.CardObjectAttributesDto;
import com.example.savetogpay.dto.GetCardDto;
import com.example.savetogpay.gpay.Config;
import com.example.savetogpay.gpay.Jwt;
import com.example.savetogpay.gpay.ResourceDefinitions;
import com.example.savetogpay.gpay.RestMethods;
import com.example.savetogpay.gpay.strategy.CardStrategy;
import com.google.api.client.json.GenericJson;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

abstract class AbstractCardStrategy implements CardStrategy {
    protected Config config = Config.getInstance();
    protected ResourceDefinitions resourceDefinitions = ResourceDefinitions.getInstance();
    protected RestMethods restMethods = RestMethods.getInstance();

    // Save link that uses JWT.
    // See https://developers.google.com/pay/passes/guides/get-started/implementing-the-api/save-to-google-pay#add-link-to-email
    private static final String SAVE_LINK = "https://pay.google.com/gp/v/save/";

    @Override
    public GetCardDto create(CardObjectAttributesDto cardObjectAttributes) throws Exception {
        if (cardObjectAttributes.getObjectId() == null) {
            String objectUid = String.format(
                    "%s_OBJECT_%s",
                    cardObjectAttributes.getCardType().toUpperCase(),
                    UUID.randomUUID().toString()
            );
            cardObjectAttributes.setObjectId(String.format("%s.%s" , config.getIssuerId(), objectUid));
        }

        GenericJson objectResponse = doCreate(cardObjectAttributes);
        GetCardDto cardDto = handleInsertCallStatusCode(objectResponse,
                cardObjectAttributes.getObjectId(), cardObjectAttributes.getClassId());

        // put into JSON Web Token (JWT) format for Google Pay API for Passes
        Jwt googlePassJwt = new Jwt();
        // only need to add objectId in JWT because class and object definitions were pre-inserted via REST call
        JsonObject jwtPayload = new JsonObject();

        jwtPayload.addProperty("id", cardDto.getObjectId());
        addObjectToGooglePassJwt(googlePassJwt, jwtPayload);

        String signedJwt = googlePassJwt.generateSignedJwt();
        cardDto.setLink(SAVE_LINK + signedJwt);
        return cardDto;
    }

    @Override
    public List<GetCardDto> getAll(String classId) throws Exception {
        return doGetAll(classId);
    }

    @Override
    public GenericJson get(String objectId) throws Exception {
        GenericJson getCallResponse = doGet(objectId);
        return handleGetCallStatusCode(getCallResponse);
    }

    @Override
    public GenericJson update(CardObjectAttributesDto cardObjectAttributes) throws Exception {
        GenericJson updateCallResponse = doUpdate(cardObjectAttributes);
        return handleUpdateCallStatusCode(updateCallResponse);
    }

    abstract GenericJson doCreate(CardObjectAttributesDto cardObjectAttributes);
    abstract void addObjectToGooglePassJwt(Jwt googlePassJwt, JsonObject jwtPayload);
    abstract List<GetCardDto> doGetAll(String classId) throws Exception;
    abstract GenericJson doGet(String objectId);
    abstract GenericJson doUpdate(CardObjectAttributesDto cardObjectAttributes);

    private GetCardDto handleInsertCallStatusCode(GenericJson insertCallResponse,
                                                  String objectId, String classId) throws Exception {
        if (insertCallResponse == null) {
            throw new Exception("Object insert issue.");
        }
        if ((int)insertCallResponse.get("code") == 200) {
            return new GetCardDto(classId, objectId);
        }
        if ((int)insertCallResponse.get("code") == 409) {  // Id resource exists for this issuer account
            throwIfClassIdOfObjectNotMatchesTargetClassId(objectId, classId);
            throw new Exception(String.format("ObjectId: (%s) already exists.", objectId));
        }
        throw new Exception("Object insert issue." + insertCallResponse.toPrettyString());
    }

    private void throwIfClassIdOfObjectNotMatchesTargetClassId(String objectId, String classId) throws Exception {
        GenericJson objectResponse = restMethods.getOfferObject(objectId);
        // check if object's classId matches target classId
        String classIdOfObjectId = (String) objectResponse.get("classId");
        if (!Objects.equals(classIdOfObjectId, classId) && classId != null) {
            throw new Exception(String.format("the classId of inserted object is (%s). " +
                    "It does not match the target classId (%s). The saved object will not " +
                    "have the class properties you expect.", classIdOfObjectId, classId));
        }
    }

    private GenericJson handleGetCallStatusCode(GenericJson getCallResponse) throws Exception {
        if (getCallResponse == null) {
            throw new Exception("Objects get issue.");
        }
        if ((int)getCallResponse.get("code") == 200) {
            return getCallResponse;
        }
        throw new Exception("Object get issue." + getCallResponse.toPrettyString());
    }

    private GenericJson handleUpdateCallStatusCode(GenericJson updateCallResponse) throws Exception {
        if (updateCallResponse == null) {
            throw new Exception("Objects update issue.");
        }
        if ((int)updateCallResponse.get("code") == 200) {
            return updateCallResponse;
        }
        throw new Exception("Object update issue." + updateCallResponse.toPrettyString());
    }
}

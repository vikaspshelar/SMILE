package com.smilecoms.rra.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.smilecoms.rra.dao.UCCDao;
import com.smilecoms.rra.model.CDRData;
import com.smilecoms.rra.model.UCCUserData;
import java.util.List;

/**
 * 
 * @author rajeshkumar
 *
 */
@Service
public class UCCDataService {

	@Autowired
	private UCCDao uccDao;

	public List<UCCUserData> getUCCUsersData() {
		return uccDao.getUsersData();
	}

	public UCCUserData getUCCUserData(String nin) {
		return uccDao.getUserData(nin);
	}

	public List<CDRData> getCDRDetails(String fromDate, String toDate) {
		return uccDao.getCDRData(fromDate,toDate);
	}

    public UCCUserData getUCCUserDataByMsisdn(String msisdn) {
        return uccDao.getUCCUserDataByMsisdn(msisdn);
    }
}

package com.smilecoms.rra.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.util.JPAUtils;
import com.smilecoms.rra.model.CDRData;
import com.smilecoms.rra.model.UCCUserData;
import com.smilecoms.rra.model.User;
import java.sql.Date;

/**
 * 
 * @author rajeshkumar
 *
 */
@Component
public class UCCDao {
	private static final Logger LOG = LoggerFactory.getLogger(UCCDao.class);
	private static Map<String, User> usersMap = null;

	/**
	 * 
	 * @param username
	 * @return
	 */
	public User findByUserName(String username) {
		if (usersMap == null || usersMap.isEmpty()) {
			loadUsers();
		}
		return usersMap.get(username);
	}

	private static void loadUsers() {
		usersMap = new HashMap<>();
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		Set<String> usersSet = BaseUtils.getPropertyAsSet("env.rra.users");
		if (usersSet == null || usersSet.isEmpty()) {
			LOG.info("env.rra.users not configured :" + usersMap.toString());
			return;
		}

		for (String user : usersSet) {
			String[] userSplit = user.split("=");
			if (userSplit.length != 2) {
				continue;
			}
			usersMap.put(userSplit[0], new User(userSplit[0], passwordEncoder.encode(userSplit[1])));
		}
		LOG.info("available users are :" + usersMap.toString());
	}

	/**
	 * 
	 * @param user
	 */
	public void saveUser(User user) {
		LOG.info("adding user " + user.getUserName() + "password : " + user.getPassword());
		usersMap.put(user.getUserName(), user);
	}

	/**
	 * 
	 * @return
	 */
	public List<UCCUserData> getUsersData() {
		PreparedStatement ps = null;
		Connection conn = null;

		String sqlQuery = BaseUtils.getProperty("env.ucc.sql.query.customers");
		if (sqlQuery == null || "".equals(sqlQuery.trim())) {
			return null;
		}
		LOG.debug("executing query :" + sqlQuery);
		Map<String, UCCUserData> usersDataMap = new HashMap<>();
		try {
			conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
			if (conn == null) {
				return null;
			}
			ps = conn.prepareStatement(sqlQuery);

			if (ps == null) {
				return null;
			}
			// 1.FIRST_NAME, 2.LAST_NAME, 3.ID_NUMBER, 4.ID_NUMBER_TYPE, 5.MSISDN, 6.ICCID
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String idNumber = rs.getString(3);
				UCCUserData userData = usersDataMap.get(idNumber);
				if (userData == null) {
					userData = new UCCUserData();
					userData.setFirstName(rs.getString(1));
					userData.setSurName(rs.getString(2));
					userData.setIdNumber(rs.getString(3));
					userData.setIdType(rs.getString(4));
				}
				userData.getMsisdnList().add(rs.getString(5));
				usersDataMap.put(idNumber, userData);
			}
		} catch (Exception ex) {
			LOG.error("error while getUsersData " + ex.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
                return new ArrayList(usersDataMap.values());
	}

	/**
	 * 
	 * @param nin
	 * @return
	 */
	public UCCUserData getUserData(String nin) {
		UCCUserData userData = new UCCUserData();
		PreparedStatement ps = null;
		Connection conn = null;
		// customer_profile where ID_NUMBER = ?";
		String sqlQuery = BaseUtils.getProperty("env.ucc.sql.query.customer");
		if (sqlQuery == null || "".equals(sqlQuery.trim())) {
			return null;
		}
		LOG.debug("executing query :" + sqlQuery);
		try {
			conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
			if (conn == null) {
				return null;
			}
			ps = conn.prepareStatement(sqlQuery);

			if (ps == null) {
				return null;
			}
			ps.setString(1, nin);
			// 1.FIRST_NAME, 2.LAST_NAME, 3.ID_NUMBER, 4.ID_NUMBER_TYPE, 5.MSISDN, 6.ICCID
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				userData.setFirstName(rs.getString(1));
				userData.setSurName(rs.getString(2));
				userData.setIdNumber(rs.getString(3));
				userData.setIdType(rs.getString(4));
				userData.getMsisdnList().add(rs.getString(5));
			}
			return userData;
		} catch (Exception ex) {
			LOG.error("error while getUsersData " + ex.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @param toDate
	 * @param fromDate
	 * @return
	 */
	public List<CDRData> getCDRData(String fromDate, String toDate) {
		PreparedStatement ps = null;
		Connection conn = null;

		String sqlQuery = BaseUtils.getProperty("env.ucc.sql.query.cdr");
		if (sqlQuery == null || "".equals(sqlQuery.trim())) {
			return null;
		}
		LOG.debug("executing query :" + sqlQuery);
		List<CDRData> cdrDataSet = new ArrayList<>();
		try {
			conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
			if (conn == null) {
				return null;
			}
			ps = conn.prepareStatement(sqlQuery);

			if (ps == null) {
				return null;
			}

			ps.setString(1, fromDate);
			ps.setString(2, toDate);
			// 1.SOURCE, 2.DESTINATION, 3.START_DATE, 4.DURATION, 5.TYPE, 6.DISPOSITION
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				CDRData cdrData = new CDRData();
				cdrData.setOriginator(getMobileNumber(rs.getString(1)));
				cdrData.setRecipient(getMobileNumber(rs.getString(2)));
				cdrData.setStartingTime(rs.getString(3));
				cdrData.setDuration(rs.getString(4));
				cdrData.setType(rs.getString(5));
				cdrData.setDisposition(rs.getString(6));
				cdrDataSet.add(cdrData);
			}
		} catch (Exception ex) {
			LOG.error("error while getCDRData" + ex.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

		return cdrDataSet;
	}

	private String getMobileNumber(String str) {
		return str.substring(str.indexOf(":") + 1, str.indexOf("@"));
	}

    public UCCUserData getUCCUserDataByMsisdn(String msisdn) {
        LOG.info("getting customers data for msisdn = "+msisdn);
        UCCUserData userData = new UCCUserData();
	PreparedStatement ps = null;
	Connection conn = null;
	// customer_profile where MSISDN = ?";
	String sqlQuery = BaseUtils.getProperty("env.ucc.sql.query.customerbymsisdn");
        LOG.debug("executing query :" + sqlQuery);
	if (sqlQuery == null || "".equals(sqlQuery.trim())) {
		return null;
	}
	try {
		conn = JPAUtils.getNonJTAConnection("jdbc/SmileDBNonJTA");
		if (conn == null) {
			return null;
		}
		ps = conn.prepareStatement(sqlQuery);
		if (ps == null) {
                    return null;
		}
		ps.setString(1, msisdn);
		// 1.FIRST_NAME, 2.LAST_NAME, 3.ID_NUMBER, 4.ID_NUMBER_TYPE, 5.MSISDN, 6.ICCID
		ResultSet rs = ps.executeQuery();
		while (rs.next()) {
                    userData.setFirstName(rs.getString(1));
                    userData.setSurName(rs.getString(2));
                    userData.setIdNumber(rs.getString(3));
                    userData.setIdType(rs.getString(4));
                    userData.getMsisdnList().add(rs.getString(5));		
                }
                    return userData;
	} catch (Exception ex) {
                    LOG.error("error while getUsersData " + ex.getMessage());
		} finally {
                    if (ps != null) {
                    	try {
                            ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
			}
                    }
                    if (conn != null) {
			try {
                            conn.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
			}
                    }
		}
		return null;  
    }

}

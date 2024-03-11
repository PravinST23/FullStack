package com.britcertify.pravin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.britcertify.pravin.dto.request.QueryDataRequest;
import com.britcertify.pravin.model.QueryData;

public interface QueryDataRepo extends JpaRepository<QueryData,String>{
    void save(QueryDataRequest enquiry);  
    List<QueryData> findByEmail(String email);  
}
package com.vitna;
import com.vitna.Domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@Service public class Access {
 final Accounts accounts;
 public Access(Accounts accounts){this.accounts=accounts;}
 public Account actor(Authentication auth,Role... roles){
  if(auth==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  Account actor=accounts.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  if(!actor.active)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  if(roles.length>0&&!Arrays.asList(roles).contains(actor.role))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  return actor;
 }
}

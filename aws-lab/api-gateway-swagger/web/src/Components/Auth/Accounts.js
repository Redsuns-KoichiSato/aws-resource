import React, { useState, createContext, useEffect } from 'react';
import { CognitoUser, AuthenticationDetails } from 'amazon-cognito-identity-js';
import Pool from '../../UserPool';


const AccountContext = createContext();

const Account = props => {

  const [LoggedIn, setLoggedIn] = useState(false);

  useEffect(() => {
    getSession()
      .then(session => {
        setLoggedIn(true);
      }, () => {
        setLoggedIn(false);
      });
  }, [])
  
  const getSession = async () =>
    await new Promise((resolve, reject) => {
      const user = Pool.getCurrentUser();
      
      if (user) {
        user.getSession(async (err, session) => {
          if (err) {
            reject();
          } else {
            const attributes = await new Promise((resolve, reject) => {
              user.getUserAttributes((err, attributes) => {
                if (err) {
                  reject(err);
                } else {
                  const results = {};

                  for (let attribute of attributes) {
                    const { Name, Value } = attribute;
                    results[Name] = Value;
                  }

                  resolve(results);
                }
              });
            });

            const token = session.getIdToken().getJwtToken()

            resolve({
              user,
              headers: {
                Authorization: token,
                "Content-Type": "application/json"
              },
              ...session,
              ...attributes
            });
          }
        });
      } else {
        reject();
      }
    });

  const authenticate = async (Username, Password) =>
    await new Promise((resolve, reject) => {
      const user = new CognitoUser({ Username, Pool });
      const authDetails = new AuthenticationDetails({ Username, Password });

      user.authenticateUser(authDetails, {
        onSuccess: data => {
          console.log('onSuccess:', data);
          resolve(data);
          setLoggedIn(true);
        },

        onFailure: err => {
          props.setError(err)
          reject(err);
        },

        newPasswordRequired: data => {
          props.setError("New Password Required")
          resolve(data);
        }
      });
    });
  
  
  const logout = () => {
    const user = Pool.getCurrentUser();
    if (user) 
      user.signOut();
      setLoggedIn(false);
  }

  return (
    <AccountContext.Provider value={{
      authenticate,
      getSession,
      logout,
      LoggedIn
    }}>
      {props.children}
    </AccountContext.Provider>
  );
};

export { Account, AccountContext };
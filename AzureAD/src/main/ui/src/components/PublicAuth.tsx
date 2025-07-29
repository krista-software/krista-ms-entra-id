import React, {useEffect, useState} from "react";
import {AuthPayload, AuthType} from "./AuthPage";
import {getCredentials} from "../api";

const PublicAuth = ({onAuthChange}: { onAuthChange: (authPayload: AuthPayload) => void }) => {
    const [email, setEmail] = useState("");
    const [savedCred, setSavedCred] = useState<boolean>(false);

    useEffect(() => {
        getAllCred().catch(error => console.log(error));
    }, []);

    useEffect(() => {
        onAuthChange({
            authType: AuthType.Public,
            email: email,
            isAllRequiredFieldsHaveValue: areFieldsValid(),
            isCredentialsSaved: isCredSaved()
        });
    }, [email]);

    const areFieldsValid = () => {
        return !!email;
    };

    const isCredSaved = (): boolean => {
        return savedCred;
    }

    const getAllCred = async () => {
        try {
            const credentials = await getCredentials(AuthType.Public);
            if (credentials) {
                setEmail(credentials["email"]);
                setSavedCred(true);
            } else {
                console.error('Failed to fetch credentials');
            }
        } catch (error) {
            console.error('Error fetching credentials:', error);
        }
    }

    return (
        <form>
            <div>
                <div
                    className="auth-form">
                    <div className="form-group">
                        <label htmlFor="email">Email<span className="mandatory-asterisk">*</span></label>
                        <input
                            type="email"
                            id="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="Enter Work or School Email Id"
                        />
                    </div>


                </div>
            </div>
        </form>
    );
};

export default PublicAuth;

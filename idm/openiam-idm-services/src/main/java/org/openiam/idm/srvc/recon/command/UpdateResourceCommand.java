package org.openiam.idm.srvc.recon.command;

import org.openiam.idm.srvc.auth.dto.Login;
import org.openiam.idm.srvc.recon.service.ReconciliationCommand;
import org.openiam.provision.dto.ProvisionUser;
import org.openiam.provision.type.ExtensibleAttribute;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Pascal
 * Date: 27.04.12
 * Time: 15:41
 * To change this template use File | Settings | File Templates.
 */
public class UpdateResourceCommand implements ReconciliationCommand {
    public boolean execute(Login login, ProvisionUser user, List<ExtensibleAttribute> attributes) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

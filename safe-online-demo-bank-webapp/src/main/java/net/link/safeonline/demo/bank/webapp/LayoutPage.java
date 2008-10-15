package net.link.safeonline.demo.bank.webapp;

import javax.ejb.EJB;

import net.link.safeonline.demo.bank.entity.BankAccountEntity;
import net.link.safeonline.demo.bank.entity.BankUserEntity;
import net.link.safeonline.demo.bank.service.AccountService;
import net.link.safeonline.demo.bank.service.TransactionService;
import net.link.safeonline.demo.bank.service.UserService;
import net.link.safeonline.demo.wicket.tools.WicketUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.link.PageLink;
import org.apache.wicket.model.Model;


public abstract class LayoutPage extends WebPage {

    private static final long serialVersionUID = 1L;
    Log                       LOG              = LogFactory.getLog(getClass());

    @EJB
    public UserService        userService;

    @EJB
    public AccountService     accountService;

    @EJB
    public TransactionService transactionService;


    /**
     * @return The userService of this {@link LayoutPage}.
     */
    UserService getUserService() {

        return this.userService;
    }

    /**
     * @return The accountService of this {@link LayoutPage}.
     */
    AccountService getAccountService() {

        return this.accountService;
    }

    /**
     * @return The transactionService of this {@link LayoutPage}.
     */
    TransactionService getTransactionService() {

        return this.transactionService;
    }

    /**
     * Add components to the layout that are present on every page.
     * 
     * This includes the title and the global ticket.
     */
    public LayoutPage() {

        add(new Label("pageTitle", "Bank Demo Application"));
        add(new Label("headerTitle", getHeaderTitle()));

        add(new UserInfo("user"));
    }

    /**
     * @return The string to use as the title for this page.
     */
    protected abstract String getHeaderTitle();


    class UserInfo extends WebMarkupContainer {

        private static final long serialVersionUID = 1L;
        private Model<String>     name;
        private Model<String>     amount;

        {
            setVisible(BankSession.isUserSet());
        }


        public UserInfo(String id) {

            super(id);

            add(getPageLink());
            add(new Link<String>("logout") {

                private static final long serialVersionUID = 1L;


                @Override
                public void onClick() {

                    getSession().invalidateNow();

                    setRedirect(true);
                    setResponsePage(LoginPage.class);
                }
            });
            add(new Label("name", this.name = new Model<String>()));
            add(new Label("amount", this.amount = new Model<String>()));

            if (BankSession.isUserSet()) {
                double total = 0;
                BankUserEntity user = BankSession.get().getUser();
                for (BankAccountEntity account : getUserService().getAccounts(user)) {
                    total += account.getAmount();
                }

                this.name.setObject(user.getName());
                this.amount.setObject(WicketUtil.format(getSession(), total));
            }
        }
    }


    Component getPageLink() {

        return new PageLink("pageLink", getPageLinkDestination());
    }

    /**
     * @return The page that the page-link refers to.
     */
    abstract Class<? extends Page> getPageLinkDestination();
}
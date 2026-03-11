/*
 * Ms Entra Id Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.stores;


import app.krista.extension.executor.SearchCondition;
import app.krista.extension.executor.SearchQuery;
import app.krista.extension.util.EntityStore;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.entities.Account;
import app.krista.extensions.cyber_security.iam.microsoft_entra_id.impl.AccountImpl;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccountStore implements EntityStore<app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.entities.Account> {
    private final AccountImpl accountImpl;

    @Inject
    public AccountStore(AccountImpl accountImpl) {
        this.accountImpl = accountImpl;
    }

    @Override
    public app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.entities.Account create(Map<String, Object> entityDefinition) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Account get(String id) throws IOException {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Failed to get contact. " + "Email_IS_EMPTY");
        }
        try {
            return accountImpl.getUser(id);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException(illegalArgumentException.getMessage());
        }
    }

    @Override
    public Account update(Account account) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String s) {
    }

    @Override
    public boolean contains(String s) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public List<Account> search(List<SearchCondition> conditions, long pageIndex, int pageSize) {
        return null;
    }

    @Override
    public List<Account> search(SearchQuery searchQuery, long l, int i) {
        List<SearchCondition> conditions = searchQuery.getSearchConditions();
        List<Account> accounts = new ArrayList<>();
        for (SearchCondition con : conditions) {
            if ("Name".equalsIgnoreCase(con.getFieldName()) || ("Email".equalsIgnoreCase(con.getFieldName())) || ("Mobile Number".equalsIgnoreCase(con.getFieldName())) || ("Id".equalsIgnoreCase(con.getFieldName()))) {
                accounts.addAll(getUsers(l, i, con));
            }
        }
        if (accounts.isEmpty() && conditions.size() == 1) {
            accounts.addAll(getUsers(l, i, null));
        }
        return accounts.stream().skip(Math.max((l - 1) * i, 0)).limit(i).collect(Collectors.toList());
    }

    private List<Account> getUsers(long l, int i, SearchCondition searchCondition) {
        UserCollectionPage list = accountImpl.getUserCollectionPage();
        List<User> users = new LinkedList<>();
        while (list != null && !list.getCurrentPage().isEmpty()) {
            users.addAll(list.getCurrentPage());
            UserCollectionRequestBuilder nextPage = list.getNextPage();
            if (nextPage == null) {
                break;
            }
            list = nextPage.buildRequest().get();
        }

        return users.stream()
                .filter(user -> {
                    if (searchCondition == null) {
                        return true;
                    } else if ("Name".equalsIgnoreCase(searchCondition.getFieldName())) {
                        return user.displayName != null && user.displayName.toLowerCase().startsWith(searchCondition.getOperand().toString().toLowerCase());
                    } else if ("Email".equalsIgnoreCase(searchCondition.getFieldName())) {
                        return user.mail != null && user.mail.toLowerCase().startsWith(searchCondition.getOperand().toString().toLowerCase());
                    } else if ("Mobile Number".equalsIgnoreCase(searchCondition.getFieldName())) {
                        return user.mobilePhone != null && user.mobilePhone.toLowerCase().startsWith(searchCondition.getOperand().toString().toLowerCase());
                    } else if ("Id".equalsIgnoreCase(searchCondition.getFieldName())) {
                        return user.id != null && user.id.toLowerCase().startsWith(searchCondition.getOperand().toString().toLowerCase());
                    }
                    return false;
                })
                .map(Account::from)
                .collect(Collectors.toList());
    }

    @Override
    public long count(List<SearchCondition> list) throws IOException {
        return 0;
    }

    @Override
    public List<String> lookup(Map<String, Object> map, List<String> list) throws IOException {
        return null;
    }

}
package org.zstack.header.identity.rbac;

import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.StatementEffect;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.BeanUtils;

import java.util.*;

public class RBAC {
    public static List<Permission> permissions = new ArrayList<>();
    public static List<Role> roles = new ArrayList<>();
    public static List<GlobalReadableResource> readableResources = new ArrayList<>();
    public static Map<Class, List<APIPermissionCheckerWrapper>> permssionCheckers = new HashMap<>();

    private static List<RoleContributor> roleContributors = new ArrayList<>();
    private static List<RoleBuilder> roleBuilders = new ArrayList<>();

    static class APIPermissionCheckerWrapper {
        boolean takeOver;
        APIPermissionChecker checker;
    }

    public static class RoleBuilder {
        private Role role = new Role();
        private List<String> permissionsByNames = new ArrayList<>();

        {
            role.setPredefine(true);
        }

        public RoleBuilder uuid(String v) {
            role.uuid = v;
            return this;
        }

        public RoleBuilder name(String v) {
            role.name = v;
            return this;
        }

        public RoleBuilder actions(String...vs) {
            role.allowedActions.addAll(Arrays.asList(vs));
            return this;
        }

        public RoleBuilder actions(Class...clzs) {
            for (Class clz : clzs) {
                role.allowedActions.add(clz.getName());
            }
            return this;
        }

        public RoleBuilder permissionsByName(String...pnames) {
            permissionsByNames.addAll(Arrays.asList(pnames));
            return this;
        }

        public RoleBuilder allow() {
            role.effect = StatementEffect.Allow;
            return this;
        }

        public RoleBuilder deny() {
            role.effect = StatementEffect.Deny;
            return this;
        }

        public RoleBuilder predefined() {
            role.predefine = true;
            return this;
        }

        public RoleBuilder notPredefined() {
            role.predefine = false;
            return this;
        }

        public RoleBuilder excludeActions(String...vs) {
            for (String v : vs) {
                role.getExcludedActions().add(v);
            }
            return this;
        }

        public RoleBuilder excludeActions(Class...clzs) {
            for (Class clz : clzs) {
                role.getExcludedActions().add(clz.getName());
            }
            return this;
        }

        public void build() {
            roleBuilders.add(this);
        }
    }

    public static class Role {
        private String uuid;
        private String name;
        private Set<String> allowedActions = new HashSet<>();
        private StatementEffect effect = StatementEffect.Allow;
        private boolean adminOnly;
        private boolean predefine = true;
        private List<String> excludedActions = new ArrayList<>();

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Set<String> getAllowedActions() {
            return allowedActions;
        }

        public void setAllowedActions(Set<String> allowedActions) {
            this.allowedActions = allowedActions;
        }

        public StatementEffect getEffect() {
            return effect;
        }

        public void setEffect(StatementEffect effect) {
            this.effect = effect;
        }

        public boolean isAdminOnly() {
            return adminOnly;
        }

        public void setAdminOnly(boolean adminOnly) {
            this.adminOnly = adminOnly;
        }

        public boolean isPredefine() {
            return predefine;
        }

        public void setPredefine(boolean predefine) {
            this.predefine = predefine;
        }

        public List<String> getExcludedActions() {
            return excludedActions;
        }

        public void setExcludedActions(List<String> excludedActions) {
            this.excludedActions = excludedActions;
        }
    }

    public static class Permission {
        private Set<String> adminOnlyAPIs = new HashSet<>();
        private Set<String> normalAPIs = new HashSet<>();
        private List<Class> targetResources = new ArrayList<>();
        private Set<String> _adminOnlyAPIs = new HashSet<>();
        private Set<String> _normalAPIs = new HashSet<>();
        private String name;

        public Set<String> getAdminOnlyAPIs() {
            return adminOnlyAPIs;
        }

        public void setAdminOnlyAPIs(Set<String> adminOnlyAPIs) {
            this.adminOnlyAPIs = adminOnlyAPIs;
        }

        public Set<String> getNormalAPIs() {
            return normalAPIs;
        }

        public void setNormalAPIs(Set<String> normalAPIs) {
            this.normalAPIs = normalAPIs;
        }

        public List<Class> getTargetResources() {
            return targetResources;
        }

        public void setTargetResources(List<Class> targetResources) {
            this.targetResources = targetResources;
        }

        public Set<String> get_adminOnlyAPIs() {
            return _adminOnlyAPIs;
        }

        public void set_adminOnlyAPIs(Set<String> _adminOnlyAPIs) {
            this._adminOnlyAPIs = _adminOnlyAPIs;
        }

        public Set<String> get_normalAPIs() {
            return _normalAPIs;
        }

        public void set_normalAPIs(Set<String> _normalAPIs) {
            this._normalAPIs = _normalAPIs;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class PermissionBuilder {
        Permission permission = new Permission();

        public PermissionBuilder name(String v) {
            permission.setName(v);
            return this;
        }

        public PermissionBuilder normalAPIs(String...vs) {
            for (String v : vs) {
                permission.get_normalAPIs().add(v);
            }

            return this;
        }

        public PermissionBuilder adminOnlyAPIs(String...vs) {
            for (String v : vs) {
                permission.get_adminOnlyAPIs().add(v);
            }

            return this;
        }

        public PermissionBuilder normalAPIs(Class...clzs) {
            for (Class clz : clzs) {
                permission.getNormalAPIs().add(clz.getName());
            }

            return this;
        }

        public PermissionBuilder adminOnlyAPIs(Class...clzs) {
            for (Class clz : clzs) {
                permission.get_adminOnlyAPIs().add(clz.getName());
            }

            return this;
        }

        public PermissionBuilder targetResources(Class...clzs) {
            for (Class clz : clzs) {
                permission.getTargetResources().add(clz);
            }

            return this;
        }

        public Permission build() {
            permission = RBACDescriptionHelper.flatten(permission);
            permissions.add(permission);
            return permission;
        }
    }

    public static class RoleContributor {
        private List<String> normalActionsByPermissionName = new ArrayList<>();
        private List<String> actions = new ArrayList<>();
        private String roleName;

        public List<String> getNormalActionsByPermissionName() {
            return normalActionsByPermissionName;
        }

        public void setNormalActionsByPermissionName(List<String> normalActionsByPermissionName) {
            this.normalActionsByPermissionName = normalActionsByPermissionName;
        }

        public List<String> getActions() {
            return actions;
        }

        public void setActions(List<String> actions) {
            this.actions = actions;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }
    }

    public static class RoleContributorBuilder {
        private RoleContributor contributor;

        public RoleContributorBuilder actionsByPermissionName(String v) {
            contributor.normalActionsByPermissionName.add(v);
            return this;
        }

        public RoleContributorBuilder actions(String...vs) {
            contributor.actions.addAll(Arrays.asList(vs));
            return this;
        }

        public RoleContributorBuilder actions(Class...clzs) {
            for (Class clz : clzs) {
                contributor.actions.add(clz.getName());
            }
            return this;
        }

        public RoleContributorBuilder roleName(String v) {
            contributor.roleName = v;
            return this;
        }

        public RoleContributor build() {
            roleContributors.add(contributor);
            return contributor;
        }
    }

    public static class GlobalReadableResourceBuilder {
        private GlobalReadableResource readableResource;

        public GlobalReadableResourceBuilder resources(Class...clzs) {
            for (Class clz : clzs) {
                readableResource.getResources().add(clz);
            }

            return this;
        }

        public void build() {
            readableResources.add(readableResource);
        }
    }

    public static class GlobalReadableResource {
        private List<Class> resources;

        public List<Class> getResources() {
            return resources;
        }

        public void setResources(List<Class> resources) {
            this.resources = resources;
        }
    }

    private static Permission findPermissionByName(String name) {
        Optional<Permission> opt = permissions.stream().filter(p->p.name.equals(name)).findFirst();
        if (!opt.isPresent()) {
            throw new CloudRuntimeException(String.format("cannot find permission[name:%s]", name));
        }
        return opt.get();
    }

    private static Role findRoleByName(String name) {
        Optional<Role> opt = roles.stream().filter(r->r.name.equals(name)).findFirst();
        if (!opt.isPresent()) {
            throw new CloudRuntimeException(String.format("cannot find role[name:%s]", name));
        }
        return opt.get();
    }

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getSubTypesOf(RBACDescription.class).forEach(dclz-> {
            RBACDescription rd;
            try {
                rd = dclz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }

            rd.permissions();
            rd.roles();
            rd.contributeToRoles();
            rd.globalReadableResources();
        });

        roleBuilders.forEach(rb -> {
            rb.permissionsByNames.forEach(pname -> {
                Permission permission = findPermissionByName(pname);
                rb.role.allowedActions.addAll(permission.getNormalAPIs());
            });

            roles.add(rb.role);
        });

        roleContributors.forEach(rc -> {
            Role role = findRoleByName(rc.roleName);
            rc.normalActionsByPermissionName.forEach(pname -> {
                Permission permission = findPermissionByName(pname);
                role.allowedActions.addAll(permission.getNormalAPIs());
            });
            role.allowedActions.addAll(rc.actions);
        });
    }

    public static boolean checkAPIPermission(APIMessage msg, boolean policyDecision) {
        List<APIPermissionCheckerWrapper> checkers = permssionCheckers.get(msg.getClass());
        if (checkers == null || checkers.isEmpty()) {
            return policyDecision;
        }

        for (APIPermissionCheckerWrapper checker : checkers) {
            Boolean ret = checker.checker.check(msg);
            if (ret == null) {
                continue;
            }

            if (checker.takeOver) {
                return ret;
            }

            if (!ret) {
                return false;
            }
        }

        return policyDecision;
    }
}

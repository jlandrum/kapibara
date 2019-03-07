function init() {
    let vm = new Vue({
        el: '#app',
        data: {
            api: null,
            selectedUrl: null,
            selectedHost: null,
            expandedAreas: {}
        },
        methods: {
            httpMethods(methods) {
                let val = {};
                ["get","put","post","delete","options","head","patch","trace"]
                    .filter(method=>methods[method]!=null).forEach(it=>val[it] = methods[it]);
                return val
            },
            toggleView(name) {
                Vue.set(vm.expandedAreas, name, !(vm.expandedAreas[name]||false))
            }
        }
    });

    window.updateContent = function(obj) {
        vm.api = obj
    }
}



